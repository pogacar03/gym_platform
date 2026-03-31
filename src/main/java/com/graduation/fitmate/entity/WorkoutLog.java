package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("workout_log")
public class WorkoutLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long planId;
    private String status;
    private Integer fatigueLevel;
    private String feedbackNote;
    private LocalDateTime completedAt;
}

