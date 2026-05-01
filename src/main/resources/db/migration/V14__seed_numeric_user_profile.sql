INSERT INTO user_profile (
    user_id,
    age,
    gender,
    height_cm,
    weight_kg,
    fitness_goal,
    activity_level,
    available_equipment,
    injury_notes,
    knee_sensitive,
    back_sensitive,
    exercise_preference,
    weekly_frequency,
    preferred_duration_minutes,
    posture_preference,
    target_areas
)
SELECT
    id,
    24,
    'PREFER_NOT_TO_SAY',
    170,
    65.00,
    'WEIGHT_LOSS',
    'BEGINNER',
    'NONE',
    'Knee-sensitive and prefers low-impact home workouts.',
    TRUE,
    TRUE,
    'LOW_IMPACT',
    3,
    20,
    'SITTING',
    'BACK,FULL_BODY'
FROM user_account
WHERE username = '123'
  AND NOT EXISTS (
      SELECT 1 FROM user_profile WHERE user_profile.user_id = user_account.id
  );
