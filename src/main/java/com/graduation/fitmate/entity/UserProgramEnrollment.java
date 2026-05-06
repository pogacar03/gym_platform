package com.graduation.fitmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_program_enrollment")
public class UserProgramEnrollment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long programId;
    private Integer currentWeek;
    private Integer currentSession;
    private Integer completedSessions;
    private Boolean active;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
