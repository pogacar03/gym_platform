CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES user_account(id),
    age INTEGER,
    gender VARCHAR(32),
    height_cm INTEGER,
    weight_kg NUMERIC(5,2),
    fitness_goal VARCHAR(32),
    activity_level VARCHAR(32),
    available_equipment VARCHAR(32),
    injury_notes VARCHAR(255),
    knee_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    back_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    exercise_preference VARCHAR(64),
    weekly_frequency INTEGER,
    preferred_duration_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workout_video (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    difficulty VARCHAR(32),
    target_goal VARCHAR(32),
    target_body_part VARCHAR(64),
    equipment_requirement VARCHAR(32),
    duration_minutes INTEGER,
    impact_level VARCHAR(16),
    safety_notes VARCHAR(255),
    video_url VARCHAR(255) NOT NULL,
    thumbnail_url VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workout_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE workout_video_tag (
    video_id BIGINT NOT NULL REFERENCES workout_video(id),
    tag_id BIGINT NOT NULL REFERENCES workout_tag(id),
    PRIMARY KEY (video_id, tag_id)
);

CREATE TABLE recommendation_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account(id),
    request_text TEXT NOT NULL,
    parsed_goal VARCHAR(32),
    parsed_duration_minutes INTEGER,
    parsed_equipment VARCHAR(32),
    safety_flags VARCHAR(255),
    explanation TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workout_plan (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account(id),
    recommendation_id BIGINT REFERENCES recommendation_history(id),
    title VARCHAR(128) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workout_plan_item (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES workout_plan(id),
    video_id BIGINT NOT NULL REFERENCES workout_video(id),
    sort_order INTEGER NOT NULL,
    sets_count INTEGER,
    reps_or_duration VARCHAR(64)
);

CREATE TABLE workout_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account(id),
    plan_id BIGINT NOT NULL REFERENCES workout_plan(id),
    status VARCHAR(32) NOT NULL,
    fatigue_level INTEGER,
    feedback_note VARCHAR(255),
    completed_at TIMESTAMP
);

CREATE INDEX idx_workout_video_goal ON workout_video(target_goal);
CREATE INDEX idx_workout_video_equipment ON workout_video(equipment_requirement);
CREATE INDEX idx_recommendation_history_user_id ON recommendation_history(user_id);
CREATE INDEX idx_workout_log_user_id ON workout_log(user_id);

