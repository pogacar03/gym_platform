package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("workout_plan_item")
public class WorkoutPlanItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long videoId;
    private Integer sortOrder;
    private Integer setsCount;
    private String repsOrDuration;
}

