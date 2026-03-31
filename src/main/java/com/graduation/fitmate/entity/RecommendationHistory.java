package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("recommendation_history")
public class RecommendationHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String requestText;
    private String parsedGoal;
    private Integer parsedDurationMinutes;
    private String parsedEquipment;
    private String safetyFlags;
    private String explanation;
    private LocalDateTime createdAt;
}

