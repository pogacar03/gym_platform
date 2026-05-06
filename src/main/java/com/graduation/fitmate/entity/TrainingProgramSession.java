package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("training_program_session")
public class TrainingProgramSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long weekId;
    private Integer sessionNumber;
    private String title;
    private Integer estimatedMinutes;
    private String intensity;
}
