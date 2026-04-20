package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.ImportedVideoSuggestion;
import com.graduation.fitmate.dto.WorkoutVideoQuery;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.WorkoutVideoMapper;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutVideoService {

    private final WorkoutVideoMapper workoutVideoMapper;
    private final ImportedVideoTaggingService importedVideoTaggingService;

    public WorkoutVideoService(WorkoutVideoMapper workoutVideoMapper, ImportedVideoTaggingService importedVideoTaggingService) {
        this.workoutVideoMapper = workoutVideoMapper;
        this.importedVideoTaggingService = importedVideoTaggingService;
    }

    public List<WorkoutVideo> findAllActive() {
        return workoutVideoMapper.selectList(new LambdaQueryWrapper<WorkoutVideo>()
                .eq(WorkoutVideo::getActive, true)
                .orderByAsc(WorkoutVideo::getDurationMinutes));
    }

    public List<WorkoutVideo> findAllActive(boolean missingOnly) {
        if (!missingOnly) {
            return findAllActive();
        }
        return findMissingTagVideos();
    }

    public List<WorkoutVideo> findCandidates(WorkoutVideoQuery query) {
        return workoutVideoMapper.findRecommendationCandidates(query);
    }

    public WorkoutVideo findById(Long id) {
        return workoutVideoMapper.selectById(id);
    }

    public WorkoutVideo findBySourceReference(String sourceType, String sourceVideoId) {
        if (sourceType == null || sourceVideoId == null || sourceVideoId.isBlank()) {
            return null;
        }
        return workoutVideoMapper.selectOne(new LambdaQueryWrapper<WorkoutVideo>()
                .eq(WorkoutVideo::getSourceType, sourceType)
                .eq(WorkoutVideo::getSourceVideoId, sourceVideoId)
                .last("limit 1"));
    }

    public List<WorkoutVideo> findMissingTagVideos() {
        return workoutVideoMapper.selectList(new LambdaQueryWrapper<WorkoutVideo>()
                .eq(WorkoutVideo::getActive, true)
                .and(wrapper -> wrapper
                        .isNull(WorkoutVideo::getTargetGoal).or().eq(WorkoutVideo::getTargetGoal, "")
                        .or().isNull(WorkoutVideo::getTargetBodyPart).or().eq(WorkoutVideo::getTargetBodyPart, "")
                        .or().isNull(WorkoutVideo::getEquipmentRequirement).or().eq(WorkoutVideo::getEquipmentRequirement, "")
                        .or().isNull(WorkoutVideo::getImpactLevel).or().eq(WorkoutVideo::getImpactLevel, "")
                        .or().isNull(WorkoutVideo::getPostureType).or().eq(WorkoutVideo::getPostureType, ""))
                .orderByAsc(WorkoutVideo::getDurationMinutes));
    }

    public int countMissingTagVideos() {
        Long count = workoutVideoMapper.selectCount(new LambdaQueryWrapper<WorkoutVideo>()
                .eq(WorkoutVideo::getActive, true)
                .and(wrapper -> wrapper
                        .isNull(WorkoutVideo::getTargetGoal).or().eq(WorkoutVideo::getTargetGoal, "")
                        .or().isNull(WorkoutVideo::getTargetBodyPart).or().eq(WorkoutVideo::getTargetBodyPart, "")
                        .or().isNull(WorkoutVideo::getEquipmentRequirement).or().eq(WorkoutVideo::getEquipmentRequirement, "")
                        .or().isNull(WorkoutVideo::getImpactLevel).or().eq(WorkoutVideo::getImpactLevel, "")
                        .or().isNull(WorkoutVideo::getPostureType).or().eq(WorkoutVideo::getPostureType, "")));
        return count == null ? 0 : count.intValue();
    }

    @Transactional
    public void autofillMissingTags(Long videoId) {
        WorkoutVideo video = workoutVideoMapper.selectById(videoId);
        if (video == null) {
            return;
        }
        ImportedVideoSuggestion suggestion = importedVideoTaggingService.suggest(new com.graduation.fitmate.entity.ImportSource(), video.getTitle(), video.getDescription());
        if (isBlank(video.getTargetGoal())) {
            video.setTargetGoal(suggestion.getGoal());
        }
        if (isBlank(video.getTargetBodyPart())) {
            video.setTargetBodyPart(suggestion.getTargetArea());
        }
        if (isBlank(video.getEquipmentRequirement())) {
            video.setEquipmentRequirement(suggestion.getEquipment());
        }
        if (isBlank(video.getImpactLevel())) {
            video.setImpactLevel(suggestion.getImpactLevel());
        }
        if (isBlank(video.getPostureType())) {
            video.setPostureType(suggestion.getPosture());
        }
        if (isBlank(video.getDifficulty())) {
            video.setDifficulty(suggestion.getDifficulty());
        }
        if (isBlank(video.getExtraTags()) && !suggestion.getExtraTags().isEmpty()) {
            video.setExtraTags(String.join(",", suggestion.getExtraTags()));
        }
        if (isBlank(video.getSafetyNotes()) && !suggestion.getSafetyFlags().isEmpty()) {
            video.setSafetyNotes(String.join(",", suggestion.getSafetyFlags()));
        }
        workoutVideoMapper.updateById(video);
    }

    @Transactional
    public int autofillMissingTags(List<Long> videoIds) {
        int updated = 0;
        for (Long id : videoIds) {
            WorkoutVideo video = workoutVideoMapper.selectById(id);
            if (video == null) {
                continue;
            }
            autofillMissingTags(id);
            updated++;
        }
        return updated;
    }

    public boolean hasMissingTags(WorkoutVideo video) {
        return Stream.of(
                        video.getTargetGoal(),
                        video.getTargetBodyPart(),
                        video.getEquipmentRequirement(),
                        video.getImpactLevel(),
                        video.getPostureType()
                )
                .anyMatch(this::isBlank);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional
    public void save(WorkoutVideo video) {
        if (video.getActive() == null) {
            video.setActive(true);
        }
        if (video.getId() == null) {
            workoutVideoMapper.insert(video);
        } else {
            workoutVideoMapper.updateById(video);
        }
    }
}
