package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("training_program")
public class TrainingProgram {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String summary;
    private String goal;
    private Integer durationWeeks;
    private Integer sessionsPerWeek;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
