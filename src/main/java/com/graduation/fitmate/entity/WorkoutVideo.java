package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("workout_video")
public class WorkoutVideo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String difficulty;
    private String targetGoal;
    private String targetBodyPart;
    private String equipmentRequirement;
    private Integer durationMinutes;
    private String impactLevel;
    private String safetyNotes;
    private String sourceType;
    private String sourceVideoId;
    private String platformChannel;
    private String embedUrl;
    private String postureType;
    private String videoUrl;
    private String thumbnailUrl;
    @TableField("is_curated")
    private Boolean curated;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
