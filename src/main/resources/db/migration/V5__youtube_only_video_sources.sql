UPDATE workout_video
SET source_type = 'YOUTUBE',
    source_video_id = CASE title
        WHEN 'Low Impact Fat Burn Walk' THEN 'FfA-DsupI6M'
        WHEN 'Chair Assisted Mobility Flow' THEN 'JuSn4MLO150'
        WHEN 'Resistance Band Upper Body Tone' THEN 'tcftFS_6B74'
        WHEN 'Core Stability Basics' THEN 'wzkQSxvHoo8'
        WHEN 'HIIT Jump Session' THEN 'qP_6WklN9PA'
        ELSE source_video_id
    END,
    video_url = CASE title
        WHEN 'Low Impact Fat Burn Walk' THEN 'https://www.youtube.com/watch?v=FfA-DsupI6M'
        WHEN 'Chair Assisted Mobility Flow' THEN 'https://www.youtube.com/watch?v=JuSn4MLO150'
        WHEN 'Resistance Band Upper Body Tone' THEN 'https://www.youtube.com/watch?v=tcftFS_6B74'
        WHEN 'Core Stability Basics' THEN 'https://www.youtube.com/watch?v=wzkQSxvHoo8'
        WHEN 'HIIT Jump Session' THEN 'https://www.youtube.com/watch?v=qP_6WklN9PA'
        ELSE video_url
    END,
    embed_url = CASE title
        WHEN 'Low Impact Fat Burn Walk' THEN 'https://www.youtube.com/embed/FfA-DsupI6M'
        WHEN 'Chair Assisted Mobility Flow' THEN 'https://www.youtube.com/embed/JuSn4MLO150'
        WHEN 'Resistance Band Upper Body Tone' THEN 'https://www.youtube.com/embed/tcftFS_6B74'
        WHEN 'Core Stability Basics' THEN 'https://www.youtube.com/embed/wzkQSxvHoo8'
        WHEN 'HIIT Jump Session' THEN 'https://www.youtube.com/embed/qP_6WklN9PA'
        ELSE embed_url
    END,
    thumbnail_url = CASE title
        WHEN 'Low Impact Fat Burn Walk' THEN 'https://img.youtube.com/vi/FfA-DsupI6M/hqdefault.jpg'
        WHEN 'Chair Assisted Mobility Flow' THEN 'https://img.youtube.com/vi/JuSn4MLO150/hqdefault.jpg'
        WHEN 'Resistance Band Upper Body Tone' THEN 'https://img.youtube.com/vi/tcftFS_6B74/hqdefault.jpg'
        WHEN 'Core Stability Basics' THEN 'https://img.youtube.com/vi/wzkQSxvHoo8/hqdefault.jpg'
        WHEN 'HIIT Jump Session' THEN 'https://img.youtube.com/vi/qP_6WklN9PA/hqdefault.jpg'
        ELSE thumbnail_url
    END,
    platform_channel = CASE title
        WHEN 'Low Impact Fat Burn Walk' THEN 'Twin Girls Fitness'
        WHEN 'Chair Assisted Mobility Flow' THEN 'Twin Girls Fitness'
        WHEN 'Resistance Band Upper Body Tone' THEN 'Twin Girls Fitness'
        WHEN 'Core Stability Basics' THEN 'Twin Girls Fitness'
        WHEN 'HIIT Jump Session' THEN 'Body Project'
        ELSE platform_channel
    END
WHERE title IN (
    'Low Impact Fat Burn Walk',
    'Chair Assisted Mobility Flow',
    'Resistance Band Upper Body Tone',
    'Core Stability Basics',
    'HIIT Jump Session'
);

