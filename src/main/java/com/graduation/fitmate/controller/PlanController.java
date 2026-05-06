package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.PlanCompletionRequest;
import com.graduation.fitmate.service.PlanService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/plans/{planId}")
    public String planSession(@PathVariable Long planId, Principal principal, Model model) {
        model.addAttribute("planSession", planService.getPlanSession(principal.getName(), planId));
        return "plans/detail";
    }

    @PostMapping("/plans/{planId}/complete")
    public String completePlan(
            @PathVariable Long planId,
            PlanCompletionRequest request,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        planService.completePlan(principal.getName(), planId, request);
        redirectAttributes.addFlashAttribute("completed", true);
        return "redirect:/plans/" + planId;
    }
}
