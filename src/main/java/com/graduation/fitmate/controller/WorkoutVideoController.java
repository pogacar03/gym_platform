package com.graduation.fitmate.controller;

import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.service.WorkoutVideoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WorkoutVideoController {

    private final WorkoutVideoService workoutVideoService;

    public WorkoutVideoController(WorkoutVideoService workoutVideoService) {
        this.workoutVideoService = workoutVideoService;
    }

    @GetMapping("/videos")
    public String videos(Model model) {
        model.addAttribute("videos", workoutVideoService.findAllActive());
        model.addAttribute("newVideo", new WorkoutVideo());
        return "videos/list";
    }

    @GetMapping("/videos/{id}")
    public String videoDetail(@PathVariable Long id, Model model) {
        WorkoutVideo video = workoutVideoService.findById(id);
        if (video == null || !Boolean.TRUE.equals(video.getActive())) {
            return "redirect:/videos";
        }
        model.addAttribute("video", video);
        return "videos/detail";
    }
}
