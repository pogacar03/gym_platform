package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.RecommendationRequest;
import com.graduation.fitmate.dto.RecommendationResult;
import com.graduation.fitmate.service.RecommendationService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public String recommendationPage(Model model) {
        if (!model.containsAttribute("recommendationRequest")) {
            model.addAttribute("recommendationRequest", new RecommendationRequest());
        }
        return "recommendation/form";
    }

    @PostMapping("/recommendations")
    public String submitRecommendation(
            @Valid @ModelAttribute("recommendationRequest") RecommendationRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "recommendation/form";
        }
        try {
            RecommendationResult result = recommendationService.recommend(principal.getName(), request.getRequestText());
            model.addAttribute("result", result);
        } catch (IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "recommendation/form";
    }
}

