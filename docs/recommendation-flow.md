# Recommendation Flow

## High-Level Flow
1. User submits free-text workout request.
2. `RequestParsingService` extracts:
   - goal
   - preferred duration
   - available equipment
   - safety concerns
3. `SafetyRuleService` converts profile and parsed request into hard filters.
4. `WorkoutVideoMapper` runs SQL retrieval with dynamic conditions.
5. `RecommendationService` picks top candidates and assembles a lightweight plan.
6. `LlmGateway` generates explanation and caution notes.
7. `RecommendationHistory` is saved for traceability.

## Fallback Rules
- If no profile exists, ask user to complete a profile before recommending.
- If LLM call fails, use template-based explanation.
- If no exact match exists, relax goal match but keep safety constraints.

## Safety Rules In V1
- `knee_sensitive = true` excludes `impact_level = HIGH`
- `back_sensitive = true` excludes videos tagged as heavy core loading
- `available_equipment = NONE` excludes equipment-required videos
- Request duration caps recommended videos to reasonable total duration

