package com.graduation.fitmate.controller;

import com.graduation.fitmate.dto.KnowledgeDocumentForm;
import com.graduation.fitmate.service.KnowledgeDocumentService;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminKnowledgeController {

    private final KnowledgeDocumentService knowledgeDocumentService;
    private final MessageSource messageSource;

    public AdminKnowledgeController(KnowledgeDocumentService knowledgeDocumentService, MessageSource messageSource) {
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.messageSource = messageSource;
    }

    @GetMapping("/admin/knowledge")
    public String knowledge(Model model) {
        return reload(model, null);
    }

    @PostMapping("/admin/knowledge")
    public String importDocument(
            @Valid @ModelAttribute("knowledgeForm") KnowledgeDocumentForm form,
            BindingResult bindingResult,
            Model model,
            Locale locale
    ) {
        if (bindingResult.hasErrors()) {
            return reload(model, message("admin.knowledge.message.invalid", locale, "Please complete the required fields."));
        }
        knowledgeDocumentService.importDocument(form);
        return reload(model, message("admin.knowledge.message.imported", locale, "Knowledge document imported and indexed."));
    }

    @PostMapping("/admin/knowledge/{id}/archive")
    public String archive(@PathVariable Long id, Model model, Locale locale) {
        knowledgeDocumentService.archiveDocument(id);
        return reload(model, message("admin.knowledge.message.archived", locale, "Knowledge document archived."));
    }

    @PostMapping("/admin/knowledge/reindex")
    public String reindex(Model model, Locale locale) {
        int count = knowledgeDocumentService.reindexAll();
        return reload(model, message("admin.knowledge.message.reindexed", new Object[]{count}, locale, "Reindexed " + count + " chunks."));
    }

    private String reload(Model model, String message) {
        model.addAttribute("documents", knowledgeDocumentService.findAllWithStats());
        if (!model.containsAttribute("knowledgeForm")) {
            model.addAttribute("knowledgeForm", new KnowledgeDocumentForm());
        }
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "admin/knowledge";
    }

    private String message(String key, Locale locale, String fallback) {
        return messageSource.getMessage(key, null, fallback, locale);
    }

    private String message(String key, Object[] args, Locale locale, String fallback) {
        return messageSource.getMessage(key, args, fallback, locale);
    }
}
