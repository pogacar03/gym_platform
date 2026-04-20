ALTER TABLE import_source
    ADD COLUMN last_import_status VARCHAR(32),
    ADD COLUMN last_import_summary VARCHAR(255);

CREATE UNIQUE INDEX uq_workout_video_source_ref
    ON workout_video(source_type, source_video_id)
    WHERE source_video_id IS NOT NULL;
