package com.graduation.fitmate.controller;

import com.graduation.fitmate.service.UserProfileService;
import com.graduation.fitmate.service.WorkoutVideoService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserProfileService userProfileService;
    private final WorkoutVideoService workoutVideoService;

    public HomeController(UserProfileService userProfileService, WorkoutVideoService workoutVideoService) {
        this.userProfileService = userProfileService;
        this.workoutVideoService = workoutVideoService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("profile", userProfileService.getProfileByUsername(principal.getName()));
        model.addAttribute("videos", workoutVideoService.findAllActive().stream().limit(3).toList());
        return "dashboard";
    }
}

