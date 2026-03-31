package com.graduation.fitmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.fitmate.dto.WorkoutVideoQuery;
import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface WorkoutVideoMapper extends BaseMapper<WorkoutVideo> {
    List<WorkoutVideo> findRecommendationCandidates(@Param("query") WorkoutVideoQuery query);
}

