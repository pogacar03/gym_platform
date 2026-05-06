package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("training_program_session_item")
public class TrainingProgramSessionItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long exerciseId;
    private Long videoId;
    private Integer sortOrder;
    private Integer setsCount;
    private String repsOrDuration;
    private Integer restSeconds;
    private String instruction;
}
