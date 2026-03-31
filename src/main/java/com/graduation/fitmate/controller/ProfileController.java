package com.graduation.fitmate.controller;

import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.service.UserProfileService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileController {

    private final UserProfileService userProfileService;

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        UserProfile profile = userProfileService.getProfileByUsername(principal.getName());
        if (profile == null) {
            profile = new UserProfile();
        }
        model.addAttribute("profile", profile);
        return "profile/form";
    }

    @PostMapping("/profile")
    public String saveProfile(@ModelAttribute("profile") UserProfile profile, Principal principal, Model model) {
        userProfileService.saveProfile(principal.getName(), profile);
        model.addAttribute("profile", userProfileService.getProfileByUsername(principal.getName()));
        model.addAttribute("message", "Profile saved successfully.");
        return "profile/form";
    }
}

