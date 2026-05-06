package com.graduation.fitmate.controller;

import com.graduation.fitmate.service.ExerciseLibraryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ExerciseController {

    private final ExerciseLibraryService exerciseLibraryService;

    public ExerciseController(ExerciseLibraryService exerciseLibraryService) {
        this.exerciseLibraryService = exerciseLibraryService;
    }

    @GetMapping("/exercises")
    public String exercises(Model model) {
        model.addAttribute("library", exerciseLibraryService.buildLibrary());
        return "exercises/list";
    }
}
