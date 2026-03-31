package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.WorkoutVideoQuery;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.WorkoutVideoMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutVideoService {

    private final WorkoutVideoMapper workoutVideoMapper;

    public WorkoutVideoService(WorkoutVideoMapper workoutVideoMapper) {
        this.workoutVideoMapper = workoutVideoMapper;
    }

    public List<WorkoutVideo> findAllActive() {
        return workoutVideoMapper.selectList(new LambdaQueryWrapper<WorkoutVideo>()
                .eq(WorkoutVideo::getActive, true)
                .orderByAsc(WorkoutVideo::getDurationMinutes));
    }

    public List<WorkoutVideo> findCandidates(WorkoutVideoQuery query) {
        return workoutVideoMapper.findRecommendationCandidates(query);
    }

    public WorkoutVideo findById(Long id) {
        return workoutVideoMapper.selectById(id);
    }

    @Transactional
    public void save(WorkoutVideo video) {
        if (video.getActive() == null) {
            video.setActive(true);
        }
        if (video.getId() == null) {
            workoutVideoMapper.insert(video);
        } else {
            workoutVideoMapper.updateById(video);
        }
    }
}
