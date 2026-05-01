package com.graduation.fitmate.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeChunkingService {

    private static final int DEFAULT_MAX_CHARS = 900;
    private static final int DEFAULT_OVERLAP_CHARS = 140;

    public List<String> chunk(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String normalized = content.replace("\r\n", "\n").trim();
        List<String> paragraphs = splitParagraphs(normalized);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.length() > DEFAULT_MAX_CHARS) {
                flush(current, chunks);
                chunks.addAll(splitLongParagraph(paragraph));
                continue;
            }
            if (!current.isEmpty() && current.length() + paragraph.length() + 2 > DEFAULT_MAX_CHARS) {
                addWithOverlap(current, chunks);
                if (!current.isEmpty() && current.length() + paragraph.length() + 2 > DEFAULT_MAX_CHARS) {
                    current.setLength(0);
                }
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }
        flush(current, chunks);
        return chunks;
    }

    public int estimateTokenCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        int latinTokens = content.trim().split("\\s+").length;
        long cjkChars = content.chars()
                .filter(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN)
                .count();
        return Math.max(latinTokens, (int) Math.ceil(cjkChars / 1.6d));
    }

    private List<String> splitParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();
        for (String paragraph : content.split("\\n\\s*\\n")) {
            String trimmed = paragraph.trim();
            if (!trimmed.isBlank()) {
                paragraphs.add(trimmed);
            }
        }
        return paragraphs;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + DEFAULT_MAX_CHARS, paragraph.length());
            chunks.add(paragraph.substring(start, end).trim());
            if (end == paragraph.length()) {
                break;
            }
            start = Math.max(end - DEFAULT_OVERLAP_CHARS, start + 1);
        }
        return chunks;
    }

    private void addWithOverlap(StringBuilder current, List<String> chunks) {
        String value = current.toString().trim();
        if (!value.isBlank()) {
            chunks.add(value);
        }
        String overlap = value.length() <= DEFAULT_OVERLAP_CHARS
                ? value
                : value.substring(value.length() - DEFAULT_OVERLAP_CHARS);
        current.setLength(0);
        if (!overlap.isBlank()) {
            current.append(overlap.trim());
        }
    }

    private void flush(StringBuilder current, List<String> chunks) {
        String value = current.toString().trim();
        if (!value.isBlank()) {
            chunks.add(value);
        }
        current.setLength(0);
    }
}
