package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.ImportSourceStats;
import com.graduation.fitmate.dto.ImportedVideoSuggestion;
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
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    public int importFromSource(Long sourceId) {
        ImportSource source = importSourceMapper.selectById(sourceId);
        if (source == null) {
            return 0;
        }
        List<ImportedVideo> fetched = fetchYoutubeChannelFeed(source);
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
            candidate.setSafetyFlags(String.join(",", suggestion.getSafetyFlags()));
            candidate.setConfidenceScore(suggestion.getConfidenceScore());
            candidate.setImportStatus("PENDING");
            importedVideoMapper.insert(candidate);
            importedCount++;

            if (Boolean.TRUE.equals(source.getAutoApproveConfident())
                    && suggestion.getConfidenceScore() != null
                    && suggestion.getConfidenceScore().compareTo(new BigDecimal("0.80")) >= 0
                    && !String.join(",", suggestion.getSafetyFlags()).contains("REVIEW")) {
                approveImportedVideo(candidate.getId(), "Auto-approved by confidence rule");
            }
        }
        source.setLastImportedAt(LocalDateTime.now());
        importSourceMapper.updateById(source);
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
    public void approveImportedVideo(Long importedVideoId, String reviewNote) {
        ImportedVideo importedVideo = importedVideoMapper.selectById(importedVideoId);
        if (importedVideo == null) {
            return;
        }

        WorkoutVideo video = new WorkoutVideo();
        video.setTitle(importedVideo.getTitle());
        video.setDescription(importedVideo.getDescription());
        video.setDifficulty(importedVideo.getSuggestedDifficulty());
        video.setTargetGoal(importedVideo.getSuggestedGoal());
        video.setTargetBodyPart(importedVideo.getSuggestedTargetArea());
        video.setEquipmentRequirement(importedVideo.getSuggestedEquipment());
        video.setDurationMinutes(15);
        video.setImpactLevel(importedVideo.getSuggestedImpactLevel());
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
        importedVideo.setReviewNote(reviewNote);
        importedVideoMapper.updateById(importedVideo);
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

    private List<ImportedVideo> fetchYoutubeChannelFeed(ImportSource source) {
        if (!"YOUTUBE_CHANNEL".equals(source.getSourceType())) {
            return List.of();
        }
        String url = "https://www.youtube.com/feeds/videos.xml?channel_id=" + source.getExternalId();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return List.of();
            }
            return parseFeed(source, response.body());
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
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
}
