UPDATE workout_video
SET
    target_goal = 'RECOVERY',
    target_body_part = 'ARMS',
    impact_level = CASE
        WHEN lower(coalesce(title, '') || ' ' || coalesce(description, '')) ~ '(pain|stiff|frozen shoulder|rotator cuff|mobility|rehab|stretch|senior)'
            THEN 'LOW'
        ELSE impact_level
    END,
    extra_tags = trim(both ',' from concat_ws(',',
        nullif(extra_tags, ''),
        CASE
            WHEN position('SHOULDER_FRIENDLY' in coalesce(extra_tags, '')) = 0 THEN 'SHOULDER_FRIENDLY'
            ELSE NULL
        END,
        CASE
            WHEN position('RECOVERY_FOCUS' in coalesce(extra_tags, '')) = 0 THEN 'RECOVERY_FOCUS'
            ELSE NULL
        END
    )),
    updated_at = CURRENT_TIMESTAMP
WHERE active = TRUE
  AND lower(coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(extra_tags, '')) ~
      '(shoulder|rotator cuff|frozen shoulder|upper body|shoulder pain)';
