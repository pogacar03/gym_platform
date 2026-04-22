package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.ImportSourceStats;
import com.graduation.fitmate.dto.ImportedVideoSuggestion;
import com.graduation.fitmate.dto.ImportedVideoReviewForm;
import com.graduation.fitmate.entity.ImportSource;
import com.graduation.fitmate.entity.ImportedVideo;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.ImportSourceMapper;
import com.graduation.fitmate.mapper.ImportedVideoMapper;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
public class VideoImportService {

    private record FeedFetchResult(List<ImportedVideo> videos, boolean failed, String message) {}

    private final ImportSourceMapper importSourceMapper;
    private final ImportedVideoMapper importedVideoMapper;
    private final ImportedVideoTaggingService taggingService;
    private final WorkoutVideoService workoutVideoService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public VideoImportService(
            ImportSourceMapper importSourceMapper,
            ImportedVideoMapper importedVideoMapper,
            ImportedVideoTaggingService taggingService,
            WorkoutVideoService workoutVideoService
    ) {
        this.importSourceMapper = importSourceMapper;
        this.importedVideoMapper = importedVideoMapper;
        this.taggingService = taggingService;
        this.workoutVideoService = workoutVideoService;
    }

    @Scheduled(cron = "0 0 */12 * * *")
    public void scheduledImport() {
        List<ImportSource> sources = importSourceMapper.selectList(new LambdaQueryWrapper<ImportSource>()
                .eq(ImportSource::getEnabled, true));
        for (ImportSource source : sources) {
            try {
                importFromSource(source.getId());
            } catch (Exception ex) {
                markSourceResult(source, "FAILED", "Scheduled import failed: " + shorten(ex.getMessage()));
            }
        }
    }

    @Transactional
    public int importFromSource(Long sourceId) {
        ImportSource source = importSourceMapper.selectById(sourceId);
        if (source == null) {
            return 0;
        }
        FeedFetchResult fetchResult = fetchYoutubeChannelFeed(source);
        if (fetchResult.failed()) {
            markSourceResult(source, "FAILED", fetchResult.message());
            return 0;
        }
        List<ImportedVideo> fetched = fetchResult.videos();
        if (fetched.isEmpty()) {
            markSourceResult(source, "NO_UPDATES", "No new videos found.");
            return 0;
        }
        int importedCount = 0;
        for (ImportedVideo candidate : fetched) {
            Long existing = importedVideoMapper.selectCount(new LambdaQueryWrapper<ImportedVideo>()
                    .eq(ImportedVideo::getSourceId, sourceId)
                    .eq(ImportedVideo::getSourceVideoId, candidate.getSourceVideoId()));
            if (existing != null && existing > 0) {
                continue;
            }
            ImportedVideoSuggestion suggestion = taggingService.suggest(source, candidate.getTitle(), candidate.getDescription());
            candidate.setSuggestedGoal(suggestion.getGoal());
            candidate.setSuggestedEquipment(suggestion.getEquipment());
            candidate.setSuggestedPosture(suggestion.getPosture());
            candidate.setSuggestedTargetArea(suggestion.getTargetArea());
            candidate.setSuggestedDifficulty(suggestion.getDifficulty());
            candidate.setSuggestedImpactLevel(suggestion.getImpactLevel());
            candidate.setSuggestedExtraTags(String.join(",", suggestion.getExtraTags()));
            candidate.setSafetyFlags(String.join(",", suggestion.getSafetyFlags()));
            candidate.setConfidenceScore(suggestion.getConfidenceScore());
            candidate.setImportStatus("PENDING");
            importedVideoMapper.insert(candidate);
            importedCount++;

            if (Boolean.TRUE.equals(source.getAutoApproveConfident())
                    && suggestion.getConfidenceScore() != null
                    && suggestion.getConfidenceScore().compareTo(new BigDecimal("0.80")) >= 0
                    && !String.join(",", suggestion.getSafetyFlags()).contains("REVIEW")) {
                ImportedVideoReviewForm reviewForm = new ImportedVideoReviewForm();
                reviewForm.setSuggestedGoal(candidate.getSuggestedGoal());
                reviewForm.setSuggestedEquipment(candidate.getSuggestedEquipment());
                reviewForm.setSuggestedPosture(candidate.getSuggestedPosture());
                reviewForm.setSuggestedTargetArea(candidate.getSuggestedTargetArea());
                reviewForm.setSuggestedDifficulty(candidate.getSuggestedDifficulty());
                reviewForm.setSuggestedImpactLevel(candidate.getSuggestedImpactLevel());
                reviewForm.setSuggestedExtraTags(candidate.getSuggestedExtraTags());
                reviewForm.setReviewNote("Auto-approved by confidence rule");
                approveImportedVideo(candidate.getId(), reviewForm);
            }
        }
        markSourceResult(source, "SUCCESS", importedCount == 0
                ? "Checked source successfully. No new staged videos."
                : "Imported " + importedCount + " new staged videos.");
        return importedCount;
    }

    public List<ImportedVideo> findLatestImported() {
        return importedVideoMapper.selectList(new LambdaQueryWrapper<ImportedVideo>()
                .orderByDesc(ImportedVideo::getCreatedAt)
                .last("limit 30"));
    }

    public List<ImportedVideo> findPending() {
        return importedVideoMapper.selectList(new LambdaQueryWrapper<ImportedVideo>()
                .eq(ImportedVideo::getImportStatus, "PENDING")
                .orderByDesc(ImportedVideo::getCreatedAt));
    }

    public Map<Long, ImportSourceStats> getSourceStats(List<ImportSource> sources) {
        Map<Long, ImportSourceStats> stats = new HashMap<>();
        for (ImportSource source : sources) {
            long total = countBySource(source.getId(), null);
            long pending = countBySource(source.getId(), "PENDING");
            long approved = countBySource(source.getId(), "APPROVED");
            long rejected = countBySource(source.getId(), "REJECTED");
            stats.put(source.getId(), new ImportSourceStats(total, pending, approved, rejected));
        }
        return stats;
    }

    @Transactional
    public void approveImportedVideo(Long importedVideoId, ImportedVideoReviewForm reviewForm) {
        ImportedVideo importedVideo = importedVideoMapper.selectById(importedVideoId);
        if (importedVideo == null) {
            return;
        }
        if ("APPROVED".equals(importedVideo.getImportStatus())) {
            return;
        }

        importedVideo.setSuggestedGoal(firstNonBlank(reviewForm.getSuggestedGoal(), importedVideo.getSuggestedGoal()));
        importedVideo.setSuggestedEquipment(firstNonBlank(reviewForm.getSuggestedEquipment(), importedVideo.getSuggestedEquipment()));
        importedVideo.setSuggestedPosture(firstNonBlank(reviewForm.getSuggestedPosture(), importedVideo.getSuggestedPosture()));
        importedVideo.setSuggestedTargetArea(firstNonBlank(reviewForm.getSuggestedTargetArea(), importedVideo.getSuggestedTargetArea()));
        importedVideo.setSuggestedDifficulty(firstNonBlank(reviewForm.getSuggestedDifficulty(), importedVideo.getSuggestedDifficulty()));
        importedVideo.setSuggestedImpactLevel(firstNonBlank(reviewForm.getSuggestedImpactLevel(), importedVideo.getSuggestedImpactLevel()));
        importedVideo.setSuggestedExtraTags(firstNonBlank(reviewForm.getSuggestedExtraTags(), importedVideo.getSuggestedExtraTags()));

        WorkoutVideo video = new WorkoutVideo();
        WorkoutVideo existing = workoutVideoService.findBySourceReference("YOUTUBE", importedVideo.getSourceVideoId());
        if (existing != null) {
            video.setId(existing.getId());
        }
        video.setTitle(importedVideo.getTitle());
        video.setDescription(importedVideo.getDescription());
        video.setDifficulty(importedVideo.getSuggestedDifficulty());
        video.setTargetGoal(importedVideo.getSuggestedGoal());
        video.setTargetBodyPart(importedVideo.getSuggestedTargetArea());
        video.setEquipmentRequirement(importedVideo.getSuggestedEquipment());
        Integer inferredDuration = taggingService.inferDurationMinutes(importedVideo.getTitle(), importedVideo.getDescription());
        if (inferredDuration != null) {
            video.setDurationMinutes(inferredDuration);
        } else if (existing != null) {
            video.setDurationMinutes(existing.getDurationMinutes());
        }
        video.setImpactLevel(importedVideo.getSuggestedImpactLevel());
        video.setExtraTags(importedVideo.getSuggestedExtraTags());
        video.setSafetyNotes(importedVideo.getSafetyFlags());
        video.setSourceType("YOUTUBE");
        video.setSourceVideoId(importedVideo.getSourceVideoId());
        video.setPlatformChannel(importedVideo.getChannelName());
        video.setEmbedUrl("https://www.youtube.com/embed/" + importedVideo.getSourceVideoId());
        video.setPostureType(importedVideo.getSuggestedPosture());
        video.setVideoUrl(importedVideo.getVideoUrl());
        video.setThumbnailUrl(importedVideo.getThumbnailUrl());
        video.setCurated(true);
        video.setActive(true);
        workoutVideoService.save(video);

        importedVideo.setImportStatus("APPROVED");
        importedVideo.setReviewNote(firstNonBlank(reviewForm.getReviewNote(), "Approved by admin"));
        importedVideoMapper.updateById(importedVideo);
    }

    @Transactional
    public int approveImportedVideos(List<Long> importedVideoIds) {
        int approvedCount = 0;
        for (Long id : importedVideoIds) {
            ImportedVideo video = importedVideoMapper.selectById(id);
            if (video == null || !"PENDING".equals(video.getImportStatus())) {
                continue;
            }
            ImportedVideoReviewForm reviewForm = new ImportedVideoReviewForm();
            reviewForm.setSuggestedGoal(video.getSuggestedGoal());
            reviewForm.setSuggestedEquipment(video.getSuggestedEquipment());
            reviewForm.setSuggestedPosture(video.getSuggestedPosture());
            reviewForm.setSuggestedTargetArea(video.getSuggestedTargetArea());
            reviewForm.setSuggestedDifficulty(video.getSuggestedDifficulty());
            reviewForm.setSuggestedImpactLevel(video.getSuggestedImpactLevel());
            reviewForm.setSuggestedExtraTags(video.getSuggestedExtraTags());
            reviewForm.setReviewNote("Batch approved by admin");
            approveImportedVideo(id, reviewForm);
            approvedCount++;
        }
        return approvedCount;
    }

    @Transactional
    public void rejectImportedVideo(Long importedVideoId, String reviewNote) {
        ImportedVideo importedVideo = importedVideoMapper.selectById(importedVideoId);
        if (importedVideo == null) {
            return;
        }
        importedVideo.setImportStatus("REJECTED");
        importedVideo.setReviewNote(reviewNote);
        importedVideoMapper.updateById(importedVideo);
    }

    private FeedFetchResult fetchYoutubeChannelFeed(ImportSource source) {
        if (!"YOUTUBE_CHANNEL".equals(source.getSourceType())) {
            return new FeedFetchResult(List.of(), true, "Unsupported source type: " + source.getSourceType());
        }
        String url = "https://www.youtube.com/feeds/videos.xml?channel_id=" + source.getExternalId();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return new FeedFetchResult(List.of(), true, "Source returned HTTP " + response.statusCode() + ".");
            }
            return new FeedFetchResult(parseFeed(source, response.body()), false, "OK");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new FeedFetchResult(List.of(), true, "Import was interrupted.");
        } catch (IOException ex) {
            return new FeedFetchResult(List.of(), true, "Could not reach source feed.");
        }
    }

    private List<ImportedVideo> parseFeed(ImportSource source, String xml) {
        List<ImportedVideo> videos = new ArrayList<>();
        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            NodeList entries = document.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                String videoId = textContent(entry, "yt:videoId");
                String title = textContent(entry, "title");
                String published = textContent(entry, "published");
                String author = textContent(entry, "name");
                if (videoId == null || title == null) {
                    continue;
                }
                ImportedVideo importedVideo = new ImportedVideo();
                importedVideo.setSourceId(source.getId());
                importedVideo.setSourceVideoId(videoId);
                importedVideo.setTitle(title);
                importedVideo.setDescription(title);
                importedVideo.setThumbnailUrl("https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg");
                importedVideo.setVideoUrl("https://www.youtube.com/watch?v=" + videoId);
                importedVideo.setChannelName(author == null ? source.getChannelName() : author);
                if (published != null) {
                    importedVideo.setPublishedAt(OffsetDateTime.parse(published, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime());
                }
                videos.add(importedVideo);
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return videos;
    }

    private String textContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private long countBySource(Long sourceId, String status) {
        LambdaQueryWrapper<ImportedVideo> wrapper = new LambdaQueryWrapper<ImportedVideo>()
                .eq(ImportedVideo::getSourceId, sourceId);
        if (status != null) {
            wrapper.eq(ImportedVideo::getImportStatus, status);
        }
        Long count = importedVideoMapper.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    private void markSourceResult(ImportSource source, String status, String summary) {
        source.setLastImportedAt(LocalDateTime.now());
        source.setLastImportStatus(status);
        source.setLastImportSummary(shorten(summary));
        importSourceMapper.updateById(source);
    }

    private String shorten(String value) {
        if (value == null || value.isBlank()) {
            return "No details available.";
        }
        return value.length() > 250 ? value.substring(0, 250) : value;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }
}
