package com.graduation.fitmate.controller;

import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.service.WorkoutVideoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminVideoController {

    private final WorkoutVideoService workoutVideoService;

    public AdminVideoController(WorkoutVideoService workoutVideoService) {
        this.workoutVideoService = workoutVideoService;
    }

    @GetMapping("/admin/videos")
    public String adminVideos(Model model) {
        model.addAttribute("videos", workoutVideoService.findAllActive());
        model.addAttribute("videoForm", new WorkoutVideo());
        return "admin/videos";
    }

    @PostMapping("/admin/videos")
    public String saveVideo(@ModelAttribute("videoForm") WorkoutVideo video, Model model) {
        workoutVideoService.save(video);
        model.addAttribute("videos", workoutVideoService.findAllActive());
        model.addAttribute("videoForm", new WorkoutVideo());
        model.addAttribute("message", "Video saved successfully.");
        return "admin/videos";
    }
}

