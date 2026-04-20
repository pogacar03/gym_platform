package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.ImportSourceForm;
import com.graduation.fitmate.dto.ImportedVideoReviewForm;
import com.graduation.fitmate.service.ImportSourceService;
import com.graduation.fitmate.service.VideoImportService;
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
public class AdminImportController {

    private final ImportSourceService importSourceService;
    private final VideoImportService videoImportService;
    private final MessageSource messageSource;

    public AdminImportController(ImportSourceService importSourceService, VideoImportService videoImportService, MessageSource messageSource) {
        this.importSourceService = importSourceService;
        this.videoImportService = videoImportService;
        this.messageSource = messageSource;
    }

    @GetMapping("/admin/imports")
    public String imports(Model model) {
        model.addAttribute("sources", importSourceService.findAll());
        model.addAttribute("sourceStats", videoImportService.getSourceStats(importSourceService.findAll()));
        model.addAttribute("pendingVideos", videoImportService.findPending());
        model.addAttribute("recentVideos", videoImportService.findLatestImported());
        model.addAttribute("sourceForm", new ImportSourceForm());
        return "admin/imports";
    }

    @PostMapping("/admin/imports/sources")
    public String saveSource(@ModelAttribute("sourceForm") ImportSourceForm form, Model model, Locale locale) {
        importSourceService.save(form);
        return reload(model, message("admin.imports.message.sourceSaved", locale, "Import source saved."));
    }

    @PostMapping("/admin/imports/sources/{id}/run")
    public String runImport(@PathVariable Long id, Model model, Locale locale) {
        int imported = videoImportService.importFromSource(id);
        return reload(model, message("admin.imports.message.run", new Object[]{imported}, locale, "Import completed. New staged videos: " + imported));
    }

    @PostMapping("/admin/imports/sources/{id}/toggle")
    public String toggleSource(@PathVariable Long id, Model model, Locale locale) {
        importSourceService.toggleEnabled(id);
        return reload(model, message("admin.imports.message.toggled", locale, "Source status updated."));
    }

    @PostMapping("/admin/imports/sources/{id}/delete")
    public String deleteSource(@PathVariable Long id, Model model, Locale locale) {
        try {
            importSourceService.deleteSource(id);
            return reload(model, message("admin.imports.message.deleted", locale, "Source deleted."));
        } catch (IllegalStateException ex) {
            return reload(model, ex.getMessage());
        }
    }

    @PostMapping("/admin/imports/videos/{id}/approve")
    public String approve(@PathVariable Long id, @ModelAttribute ImportedVideoReviewForm reviewForm, Model model, Locale locale) {
        videoImportService.approveImportedVideo(id, reviewForm);
        return reload(model, message("admin.imports.message.approved", locale, "Imported video approved."));
    }

    @PostMapping("/admin/imports/videos/{id}/reject")
    public String reject(@PathVariable Long id, Model model, Locale locale) {
        videoImportService.rejectImportedVideo(id, "Rejected by admin");
        return reload(model, message("admin.imports.message.rejected", locale, "Imported video rejected."));
    }

    @PostMapping("/admin/imports/videos/batch-approve")
    public String batchApprove(@RequestParam(name = "selectedIds", required = false) java.util.List<Long> selectedIds, Model model, Locale locale) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return reload(model, message("admin.imports.message.selectFirst", locale, "Select at least one pending video first."));
        }
        int approved = videoImportService.approveImportedVideos(selectedIds);
        return reload(model, message("admin.imports.message.batchApproved", new Object[]{approved}, locale, "Batch approved " + approved + " imported videos."));
    }

    private String reload(Model model, String message) {
        model.addAttribute("message", message);
        model.addAttribute("sources", importSourceService.findAll());
        model.addAttribute("sourceStats", videoImportService.getSourceStats(importSourceService.findAll()));
        model.addAttribute("pendingVideos", videoImportService.findPending());
        model.addAttribute("recentVideos", videoImportService.findLatestImported());
        model.addAttribute("sourceForm", new ImportSourceForm());
        return "admin/imports";
    }

    private String message(String key, Locale locale, String fallback) {
        return messageSource.getMessage(key, null, fallback, locale);
    }

    private String message(String key, Object[] args, Locale locale, String fallback) {
        return messageSource.getMessage(key, args, fallback, locale);
    }
}
