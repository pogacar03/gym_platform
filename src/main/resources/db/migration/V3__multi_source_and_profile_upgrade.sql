ALTER TABLE user_profile
    ADD COLUMN posture_preference VARCHAR(32),
    ADD COLUMN target_areas VARCHAR(255);

ALTER TABLE workout_video
    ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'YOUTUBE',
    ADD COLUMN source_video_id VARCHAR(64),
    ADD COLUMN platform_channel VARCHAR(128),
    ADD COLUMN embed_url VARCHAR(255),
    ADD COLUMN posture_type VARCHAR(32),
    ADD COLUMN is_curated BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE user_profile
SET posture_preference = 'SITTING',
    target_areas = 'FULL_BODY'
WHERE user_id IN (SELECT id FROM user_account WHERE username = 'student');

UPDATE workout_video
SET source_type = 'YOUTUBE',
    platform_channel = CASE title
        WHEN 'Low Impact Fat Burn Walk' THEN 'Get Fit with Rick'
        WHEN 'Chair Assisted Mobility Flow' THEN 'More Life Health Seniors'
        WHEN 'Resistance Band Upper Body Tone' THEN 'Ask Doctor Jo'
        WHEN 'Core Stability Basics' THEN 'Move With Nicole'
        ELSE 'Body Project'
    END,
    posture_type = CASE title
        WHEN 'Chair Assisted Mobility Flow' THEN 'SITTING'
        WHEN 'Resistance Band Upper Body Tone' THEN 'STANDING'
        WHEN 'Core Stability Basics' THEN 'FLOOR'
        ELSE 'STANDING'
    END,
    video_url = CASE title
        WHEN 'Low Impact Fat Burn Walk' THEN 'https://www.youtube.com/results?search_query=low+impact+walking+workout+get+fit+with+rick'
        WHEN 'Chair Assisted Mobility Flow' THEN 'https://www.youtube.com/results?search_query=chair+mobility+workout+more+life+health+seniors'
        WHEN 'Resistance Band Upper Body Tone' THEN 'https://www.youtube.com/results?search_query=resistance+band+upper+body+workout+ask+doctor+jo'
        WHEN 'Core Stability Basics' THEN 'https://www.youtube.com/results?search_query=beginner+core+stability+workout+move+with+nicole'
        ELSE 'https://www.youtube.com/results?search_query=hiit+jump+workout+body+project'
    END
WHERE source_type = 'YOUTUBE';

