package com.graduation.fitmate.controller;

import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.service.WorkoutVideoService;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminVideoController {

    private final WorkoutVideoService workoutVideoService;
    private final MessageSource messageSource;

    public AdminVideoController(WorkoutVideoService workoutVideoService, MessageSource messageSource) {
        this.workoutVideoService = workoutVideoService;
        this.messageSource = messageSource;
    }

    @GetMapping("/admin/videos")
    public String adminVideos(@RequestParam(name = "missingOnly", defaultValue = "false") boolean missingOnly, Model model, Locale locale) {
        return render(model, message("admin.videos.message.overview", locale, "Library overview"), missingOnly);
    }

    @PostMapping("/admin/videos")
    public String saveVideo(@ModelAttribute("videoForm") WorkoutVideo video, Model model, Locale locale) {
        workoutVideoService.save(video);
        return render(model, message("admin.videos.message.saved", locale, "Video saved successfully."), false);
    }

    @PostMapping("/admin/videos/{id}/autofill-tags")
    public String autofillTags(@PathVariable Long id, Model model, Locale locale) {
        workoutVideoService.autofillMissingTags(id);
        return render(model, message("admin.videos.message.autofilled", locale, "Missing tags auto-filled where possible."), true);
    }

    @PostMapping("/admin/videos/batch-autofill-tags")
    public String batchAutofillTags(@RequestParam(name = "selectedIds", required = false) java.util.List<Long> selectedIds, Model model, Locale locale) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return render(model, message("admin.videos.message.selectFirst", locale, "Select at least one video before running bulk auto-fill."), true);
        }
        int updated = workoutVideoService.autofillMissingTags(selectedIds);
        return render(model, message("admin.videos.message.bulkFilled", new Object[]{updated}, locale, "Bulk auto-filled tags for " + updated + " videos."), true);
    }

    private String render(Model model, String message, boolean missingOnly) {
        model.addAttribute("videos", workoutVideoService.findAllActive(missingOnly));
        model.addAttribute("missingTagVideos", workoutVideoService.findMissingTagVideos());
        model.addAttribute("missingTagCount", workoutVideoService.countMissingTagVideos());
        model.addAttribute("missingOnly", missingOnly);
        model.addAttribute("videoForm", new WorkoutVideo());
        model.addAttribute("message", message);
        return "admin/videos";
    }

    private String message(String key, Locale locale, String fallback) {
        return messageSource.getMessage(key, null, fallback, locale);
    }

    private String message(String key, Object[] args, Locale locale, String fallback) {
        return messageSource.getMessage(key, args, fallback, locale);
    }
}
