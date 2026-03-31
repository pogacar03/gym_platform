INSERT INTO user_account (username, password_hash, role, display_name)
VALUES
('student', '{noop}password123', 'ROLE_USER', 'Demo Student'),
('admin', '{noop}password123', 'ROLE_ADMIN', 'Demo Admin');

INSERT INTO user_profile (
    user_id, age, gender, height_cm, weight_kg, fitness_goal, activity_level,
    available_equipment, injury_notes, knee_sensitive, back_sensitive,
    exercise_preference, weekly_frequency, preferred_duration_minutes
)
SELECT id, 22, 'PREFER_NOT_TO_SAY', 170, 65.00, 'WEIGHT_LOSS', 'BEGINNER',
       'NONE', 'Right knee feels uncomfortable during jumping.', TRUE, FALSE,
       'LOW_IMPACT', 3, 20
FROM user_account
WHERE username = 'student';

INSERT INTO workout_video (
    title, description, difficulty, target_goal, target_body_part, equipment_requirement,
    duration_minutes, impact_level, safety_notes, video_url, thumbnail_url
)
VALUES
('Low Impact Fat Burn Walk', 'Gentle cardio session for small spaces.', 'BEGINNER', 'WEIGHT_LOSS', 'FULL_BODY', 'NONE', 12, 'LOW', 'Suitable when avoiding jumping.', 'https://example.com/videos/low-impact-fat-burn', 'https://placehold.co/320x180'),
('Chair Assisted Mobility Flow', 'Mobility and recovery routine with chair support.', 'BEGINNER', 'RECOVERY', 'FULL_BODY', 'CHAIR', 15, 'LOW', 'Good choice for recovery days.', 'https://example.com/videos/chair-mobility', 'https://placehold.co/320x180'),
('Resistance Band Upper Body Tone', 'Upper-body endurance workout with bands.', 'INTERMEDIATE', 'MUSCLE_TONE', 'UPPER_BODY', 'BANDS', 20, 'MEDIUM', 'Avoid if shoulder pain increases.', 'https://example.com/videos/band-upper-body', 'https://placehold.co/320x180'),
('Core Stability Basics', 'Controlled core routine with low back awareness.', 'BEGINNER', 'CORE_STRENGTH', 'CORE', 'NONE', 10, 'LOW', 'Keep neutral spine during each movement.', 'https://example.com/videos/core-stability', 'https://placehold.co/320x180'),
('HIIT Jump Session', 'High-energy fat loss routine with repeated jumps.', 'INTERMEDIATE', 'WEIGHT_LOSS', 'FULL_BODY', 'NONE', 18, 'HIGH', 'Not suitable for knee-sensitive users.', 'https://example.com/videos/hiit-jump-session', 'https://placehold.co/320x180');
