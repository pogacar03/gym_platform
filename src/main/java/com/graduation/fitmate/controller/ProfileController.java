package com.graduation.fitmate.controller;

import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.service.UserProfileService;
import java.security.Principal;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileController {

    private final UserProfileService userProfileService;
    private final MessageSource messageSource;

    public ProfileController(UserProfileService userProfileService, MessageSource messageSource) {
        this.userProfileService = userProfileService;
        this.messageSource = messageSource;
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
    public String saveProfile(@ModelAttribute("profile") UserProfile profile, Principal principal, Model model, Locale locale) {
        userProfileService.saveProfile(principal.getName(), profile);
        model.addAttribute("profile", userProfileService.getProfileByUsername(principal.getName()));
        model.addAttribute("message", messageSource.getMessage(
                "profile.saved",
                null,
                "Profile saved successfully.",
                locale
        ));
        return "profile/form";
    }
}
