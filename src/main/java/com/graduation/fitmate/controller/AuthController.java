package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.RegisterRequest;
import com.graduation.fitmate.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            registrationService.register(request);
            model.addAttribute("message", "Registration completed. Please log in.");
            model.addAttribute("registerRequest", new RegisterRequest());
            return "login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }
}

