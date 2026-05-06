package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exercise_video")
public class ExerciseVideo {
    private Long exerciseId;
    private Long videoId;
}
