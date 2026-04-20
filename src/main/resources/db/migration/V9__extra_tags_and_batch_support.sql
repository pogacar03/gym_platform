ALTER TABLE workout_video
    ADD COLUMN extra_tags VARCHAR(255);

ALTER TABLE imported_video
    ADD COLUMN suggested_extra_tags VARCHAR(255);
