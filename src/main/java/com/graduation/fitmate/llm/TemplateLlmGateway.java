package com.graduation.fitmate.llm;

import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class TemplateLlmGateway implements LlmGateway {

    @Override
    public String generateExplanation(UserProfile profile, ParsedRecommendationRequest parsedRequest, List<WorkoutVideo> videos) {
        String titles = videos.stream().map(WorkoutVideo::getTitle).collect(Collectors.joining(", "));
        StringBuilder builder = new StringBuilder();
        builder.append("Based on your goal of ")
                .append(parsedRequest.getGoal())
                .append(" and a target duration around ")
                .append(parsedRequest.getDurationMinutes())
                .append(" minutes, these sessions were selected from the curated library: ")
                .append(titles)
                .append(". ");
        if (parsedRequest.getPostureType() != null) {
            builder.append("The recommendation respects your preferred exercise posture of ")
                    .append(parsedRequest.getPostureType())
                    .append(". ");
        }
        if (parsedRequest.getTargetArea() != null) {
            builder.append("The selected videos also lean toward ")
                    .append(parsedRequest.getTargetArea())
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
        builder.append("If any movement causes pain, stop and switch to the gentlest option.");
        return builder.toString();
    }
}
