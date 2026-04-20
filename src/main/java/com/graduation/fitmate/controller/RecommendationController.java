package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.RecommendationRequest;
import com.graduation.fitmate.dto.RecommendationResult;
import com.graduation.fitmate.service.RecommendationService;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final MessageSource messageSource;

    public RecommendationController(RecommendationService recommendationService, MessageSource messageSource) {
        this.recommendationService = recommendationService;
        this.messageSource = messageSource;
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
            @ModelAttribute("recommendationRequest") RecommendationRequest request,
            Principal principal,
            Model model,
            Locale locale
    ) {
        String composedRequest = composeRequest(request);
        if (composedRequest.isBlank()) {
            model.addAttribute("error", messageSource.getMessage(
                    "recommend.error.empty",
                    null,
                    "Please describe your workout need or choose quick filters first.",
                    locale
            ));
            return "recommendation/form";
        }
        try {
            RecommendationResult result = recommendationService.recommend(principal.getName(), composedRequest);
            model.addAttribute("result", result);
        } catch (IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "recommendation/form";
    }

    private String composeRequest(RecommendationRequest request) {
        List<String> parts = new ArrayList<>();
        if (request.getQuickDurationMinutes() != null) {
            parts.add(request.getQuickDurationMinutes() + " minutes");
        }
        if (request.getQuickPosture() != null && !request.getQuickPosture().isBlank()) {
            parts.add(request.getQuickPosture());
        }
        if (request.getQuickEquipment() != null && !request.getQuickEquipment().isBlank()) {
            parts.add(request.getQuickEquipment());
        }
        if (request.getQuickIntensity() != null && !request.getQuickIntensity().isBlank()) {
            parts.add(request.getQuickIntensity());
        }
        if (request.getQuickTargetArea() != null && !request.getQuickTargetArea().isBlank()) {
            parts.add(request.getQuickTargetArea());
        }
        if (request.getRequestText() != null && !request.getRequestText().isBlank()) {
            parts.add(request.getRequestText().trim());
        }
        return String.join(", ", parts).trim();
    }
}
