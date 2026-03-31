package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("imported_video")
public class ImportedVideo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sourceId;
    private String sourceVideoId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private String channelName;
    private LocalDateTime publishedAt;
    private String importStatus;
    private String suggestedGoal;
    private String suggestedEquipment;
    private String suggestedPosture;
    private String suggestedTargetArea;
    private String suggestedDifficulty;
    private String suggestedImpactLevel;
    private String safetyFlags;
    private BigDecimal confidenceScore;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

