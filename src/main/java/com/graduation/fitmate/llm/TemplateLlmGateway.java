package com.graduation.fitmate.llm;

import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.RecommendationKnowledgeNote;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Primary;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@Primary
public class TemplateLlmGateway implements LlmGateway {

    private final MessageSource messageSource;

    public TemplateLlmGateway(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String generateExplanation(
            UserProfile profile,
            ParsedRecommendationRequest parsedRequest,
            List<WorkoutVideo> videos,
            List<RecommendationKnowledgeNote> knowledgeNotes
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        String titles = videos.stream().map(WorkoutVideo::getTitle).collect(Collectors.joining(", "));
        if (locale != null && locale.getLanguage().toLowerCase(Locale.ROOT).startsWith("zh")) {
            StringBuilder zhBuilder = new StringBuilder();
            zhBuilder.append("这组推荐基于你的训练目标“")
                    .append(localizeLabel(parsedRequest.getGoal(), locale))
                    .append("”、预计时长约 ")
                    .append(parsedRequest.getDurationMinutes())
                    .append(" 分钟，以及当前可用的视频库综合整理而成，当前最匹配的视频包括：")
                    .append(titles)
                    .append("。");
            if (parsedRequest.getPostureType() != null) {
                zhBuilder.append("系统保留了你偏好的")
                        .append(localizeLabel(parsedRequest.getPostureType(), locale))
                        .append("训练方式。");
            }
            if (parsedRequest.getTargetArea() != null) {
                zhBuilder.append("视频重点也尽量贴合你当前关注的")
                        .append(localizeLabel(parsedRequest.getTargetArea(), locale))
                        .append("。");
            }
            if (parsedRequest.isKneeSensitive()) {
                zhBuilder.append("考虑到膝盖敏感，系统优先排除了高冲击内容。");
            }
            if (parsedRequest.isBackSensitive()) {
                zhBuilder.append("考虑到背部敏感，系统优先保留更稳定、更可控的动作。");
            }
            if (profile.getExercisePreference() != null) {
                zhBuilder.append("同时也参考了你偏好的训练风格：")
                        .append(localizeLabel(profile.getExercisePreference(), locale))
                        .append("。");
            }
            if (!knowledgeNotes.isEmpty()) {
                zhBuilder.append("这次还额外补充了几条训练建议：")
                        .append(knowledgeNotes.stream()
                                .map(RecommendationKnowledgeNote::getTitle)
                                .collect(Collectors.joining("、")))
                        .append("。");
            }
            zhBuilder.append("如果任何动作让疼痛明显加重，请立即停止并切换到更温和的版本。");
            return zhBuilder.toString();
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Based on your goal of ")
                .append(humanizeGoal(parsedRequest.getGoal()))
                .append(" and a target duration around ")
                .append(parsedRequest.getDurationMinutes())
                .append(" minutes, these sessions were selected from the curated library: ")
                .append(titles)
                .append(". ");
        if (parsedRequest.getPostureType() != null) {
            builder.append("The recommendation respects your preferred exercise posture of ")
                    .append(humanizeEnum(parsedRequest.getPostureType()))
                    .append(". ");
        }
        if (parsedRequest.getTargetArea() != null) {
            builder.append("The selected videos also lean toward ")
                    .append(humanizeEnum(parsedRequest.getTargetArea()))
                    .append(" focus. ");
        }
        if (parsedRequest.isKneeSensitive()) {
            builder.append("High-impact workouts were excluded because knee sensitivity was detected. ");
        }
        if (parsedRequest.isBackSensitive()) {
            builder.append("Back-loading movements should be monitored carefully. ");
        }
        if (profile.getExercisePreference() != null) {
            builder.append("The plan also considers your preference for ")
                    .append(profile.getExercisePreference())
                    .append(". ");
        }
        if (!knowledgeNotes.isEmpty()) {
            builder.append("Coach notes highlighted in this plan include ");
            builder.append(knowledgeNotes.stream()
                    .map(RecommendationKnowledgeNote::getTitle)
                    .collect(Collectors.joining(", ")));
            builder.append(". ");
        }
        builder.append("If any movement causes pain, stop and switch to the gentlest option.");
        return builder.toString();
    }

    private String humanizeGoal(String goal) {
        if (goal == null) {
            return "general fitness";
        }
        return switch (goal.toUpperCase(Locale.ROOT)) {
            case "WEIGHT_LOSS" -> "weight loss";
            case "MUSCLE_TONE" -> "muscle tone";
            case "RECOVERY" -> "recovery";
            case "CORE_STRENGTH" -> "core strength";
            default -> goal.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    private String humanizeEnum(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String localizeLabel(String value, Locale locale) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String key = "label." + value.toLowerCase(Locale.ROOT);
        return messageSource.getMessage(key, null, humanizeEnum(value), locale);
    }
}
