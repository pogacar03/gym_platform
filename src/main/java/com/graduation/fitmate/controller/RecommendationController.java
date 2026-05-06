package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.RecommendationRequest;
import com.graduation.fitmate.dto.RecommendationResult;
import com.graduation.fitmate.service.RecommendationService;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RecommendationController {

    private static final Pattern MEANINGFUL_TEXT_PATTERN = Pattern.compile("[\\p{L}\\p{IsHan}]{2,}");

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
        if (!hasMeaningfulTrainingSignal(request, composedRequest)) {
            model.addAttribute("error", messageSource.getMessage(
                    "recommend.error.vague",
                    null,
                    "Please describe a real workout goal, body area, limitation, equipment, or choose more quick filters.",
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
        if (request.getQuickSafety() != null && !request.getQuickSafety().isBlank()) {
            parts.add(request.getQuickSafety());
        }
        if (request.getRequestText() != null && !request.getRequestText().isBlank()) {
            parts.add(request.getRequestText().trim());
        }
        return String.join(", ", parts).trim();
    }

    private boolean hasMeaningfulTrainingSignal(RecommendationRequest request, String composedRequest) {
        int quickSignalCount = 0;
        if (hasText(request.getQuickPosture())) {
            quickSignalCount++;
        }
        if (hasText(request.getQuickEquipment())) {
            quickSignalCount++;
        }
        if (hasText(request.getQuickIntensity())) {
            quickSignalCount++;
        }
        if (hasText(request.getQuickTargetArea())) {
            quickSignalCount++;
        }
        if (hasText(request.getQuickSafety())) {
            quickSignalCount++;
        }
        if (quickSignalCount >= 1) {
            return true;
        }
        String freeText = request.getRequestText() == null ? "" : request.getRequestText().trim();
        return MEANINGFUL_TEXT_PATTERN.matcher(freeText).find()
                && containsTrainingIntent((freeText + " " + composedRequest).toLowerCase(Locale.ROOT));
    }

    private boolean containsTrainingIntent(String text) {
        return List.of(
                "训练", "锻炼", "健身", "减脂", "燃脂", "增肌", "塑形", "康复", "恢复", "缓解", "疼", "痛",
                "肩", "背", "腰", "膝", "腿", "臀", "核心", "手臂", "胸", "拉伸", "放松",
                "workout", "exercise", "training", "fitness", "mobility", "recovery", "rehab", "stretch",
                "pain", "shoulder", "back", "knee", "core", "arms", "legs", "glutes", "chest"
        ).stream().anyMatch(text::contains);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
