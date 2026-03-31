package com.graduation.fitmate.llm;

import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.List;

public interface LlmGateway {
    String generateExplanation(UserProfile profile, ParsedRecommendationRequest parsedRequest, List<WorkoutVideo> videos);
}

