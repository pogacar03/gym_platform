package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.ImportSourceForm;
import com.graduation.fitmate.service.ImportSourceService;
import com.graduation.fitmate.service.VideoImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminImportController {

    private final ImportSourceService importSourceService;
    private final VideoImportService videoImportService;

    public AdminImportController(ImportSourceService importSourceService, VideoImportService videoImportService) {
        this.importSourceService = importSourceService;
        this.videoImportService = videoImportService;
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
    public String saveSource(@ModelAttribute("sourceForm") ImportSourceForm form, Model model) {
        importSourceService.save(form);
        return reload(model, "Import source saved.");
    }

    @PostMapping("/admin/imports/sources/{id}/run")
    public String runImport(@PathVariable Long id, Model model) {
        int imported = videoImportService.importFromSource(id);
        return reload(model, "Import completed. New staged videos: " + imported);
    }

    @PostMapping("/admin/imports/sources/{id}/toggle")
    public String toggleSource(@PathVariable Long id, Model model) {
        importSourceService.toggleEnabled(id);
        return reload(model, "Source status updated.");
    }

    @PostMapping("/admin/imports/sources/{id}/delete")
    public String deleteSource(@PathVariable Long id, Model model) {
        try {
            importSourceService.deleteSource(id);
            return reload(model, "Source deleted.");
        } catch (IllegalStateException ex) {
            return reload(model, ex.getMessage());
        }
    }

    @PostMapping("/admin/imports/videos/{id}/approve")
    public String approve(@PathVariable Long id, Model model) {
        videoImportService.approveImportedVideo(id, "Approved by admin");
        return reload(model, "Imported video approved.");
    }

    @PostMapping("/admin/imports/videos/{id}/reject")
    public String reject(@PathVariable Long id, Model model) {
        videoImportService.rejectImportedVideo(id, "Rejected by admin");
        return reload(model, "Imported video rejected.");
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
}
