package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("training_program_week")
public class TrainingProgramWeek {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long programId;
    private Integer weekNumber;
    private String title;
    private String focus;
    private String notes;
}
