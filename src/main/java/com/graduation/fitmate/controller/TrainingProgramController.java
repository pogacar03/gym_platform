package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.TrainingProgramView;
import com.graduation.fitmate.service.TrainingProgramService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TrainingProgramController {

    private final TrainingProgramService trainingProgramService;

    public TrainingProgramController(TrainingProgramService trainingProgramService) {
        this.trainingProgramService = trainingProgramService;
    }

    @GetMapping("/programs")
    public String programs(Principal principal, Model model) {
        TrainingProgramView program = trainingProgramService.latestProgram(principal.getName());
        model.addAttribute("programView", program);
        return "programs/detail";
    }

    @PostMapping("/programs/generate")
    public String generateProgram(Principal principal, Model model) {
        model.addAttribute("programView", trainingProgramService.generateFourWeekProgram(principal.getName()));
        model.addAttribute("generated", true);
        return "programs/detail";
    }
}
