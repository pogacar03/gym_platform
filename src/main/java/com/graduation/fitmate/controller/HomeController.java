package com.graduation.fitmate.controller;

import com.graduation.fitmate.service.DashboardService;
import com.graduation.fitmate.service.UserProfileService;
import com.graduation.fitmate.service.WorkoutVideoService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    private final UserProfileService userProfileService;
    private final WorkoutVideoService workoutVideoService;
    private final DashboardService dashboardService;

    public HomeController(UserProfileService userProfileService, WorkoutVideoService workoutVideoService, DashboardService dashboardService) {
        this.userProfileService = userProfileService;
        this.workoutVideoService = workoutVideoService;
        this.dashboardService = dashboardService;
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
        model.addAttribute("dashboard", dashboardService.buildDashboard(principal.getName()));
        return "dashboard";
    }

    @PostMapping("/dashboard/complete-latest")
    public String completeLatestPlan(Principal principal) {
        dashboardService.completeLatestPlan(principal.getName());
        return "redirect:/dashboard";
    }
}
