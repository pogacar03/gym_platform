package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("exercise")
public class Exercise {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String primaryMuscle;
    private String secondaryMuscles;
    private String equipment;
    private String exerciseType;
    private String mechanicsType;
    private String difficulty;
    private String riskLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
