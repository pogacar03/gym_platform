package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("workout_plan")
public class WorkoutPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long recommendationId;
    private String title;
    private String summary;
    private LocalDateTime createdAt;
}

