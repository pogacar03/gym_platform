UPDATE workout_video
SET video_url = CASE title
    WHEN 'Low Impact Fat Burn Walk' THEN 'https://www.youtube.com/watch?v=L0bHG-58M3I&list=PLsPoR5SimhuWMVSEUKGMLxGPLh28GSxRy'
    WHEN 'Chair Assisted Mobility Flow' THEN 'https://www.improvedhealthforseniors.com/exercise'
    WHEN 'Resistance Band Upper Body Tone' THEN 'https://www.askdoctorjo.com/wellness-challenge-upper-dynamics-moderate/'
    WHEN 'Core Stability Basics' THEN 'https://www.fitnessblender.com/videos/quick-glute-and-core-tune-up-build-strength-and-stability'
    WHEN 'HIIT Jump Session' THEN 'https://www.youtube.com/watch?v=qP_6WklN9PA&list=PLsPoR5SimhuUiihswpW7KxHlYEKY5HVCG'
    ELSE video_url
END,
platform_channel = CASE title
    WHEN 'Low Impact Fat Burn Walk' THEN 'Improved Health'
    WHEN 'Chair Assisted Mobility Flow' THEN 'Improved Health'
    WHEN 'Resistance Band Upper Body Tone' THEN 'Ask Doctor Jo'
    WHEN 'Core Stability Basics' THEN 'Fitness Blender'
    WHEN 'HIIT Jump Session' THEN 'Improved Health'
    ELSE platform_channel
END
WHERE title IN (
    'Low Impact Fat Burn Walk',
    'Chair Assisted Mobility Flow',
    'Resistance Band Upper Body Tone',
    'Core Stability Basics',
    'HIIT Jump Session'
);

INSERT INTO user_account (username, password_hash, role, display_name, enabled)
SELECT 'stu', '{noop}123', 'ROLE_USER', 'Test Student', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM user_account WHERE username = 'stu'
);
