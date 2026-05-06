CREATE TABLE IF NOT EXISTS exercise (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    primary_muscle VARCHAR(64),
    secondary_muscles VARCHAR(255),
    equipment VARCHAR(64),
    exercise_type VARCHAR(64),
    mechanics_type VARCHAR(64),
    difficulty VARCHAR(32),
    risk_level VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_exercise_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS exercise_video (
    exercise_id BIGINT NOT NULL REFERENCES exercise(id) ON DELETE CASCADE,
    video_id BIGINT NOT NULL REFERENCES workout_video(id) ON DELETE CASCADE,
    PRIMARY KEY (exercise_id, video_id)
);

CREATE TABLE IF NOT EXISTS training_program (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account(id),
    title VARCHAR(160) NOT NULL,
    summary TEXT,
    goal VARCHAR(64),
    duration_weeks INTEGER NOT NULL DEFAULT 4,
    sessions_per_week INTEGER NOT NULL DEFAULT 3,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS training_program_week (
    id BIGSERIAL PRIMARY KEY,
    program_id BIGINT NOT NULL REFERENCES training_program(id) ON DELETE CASCADE,
    week_number INTEGER NOT NULL,
    title VARCHAR(160) NOT NULL,
    focus VARCHAR(160),
    notes TEXT,
    CONSTRAINT uk_training_program_week UNIQUE (program_id, week_number)
);

CREATE TABLE IF NOT EXISTS training_program_session (
    id BIGSERIAL PRIMARY KEY,
    week_id BIGINT NOT NULL REFERENCES training_program_week(id) ON DELETE CASCADE,
    session_number INTEGER NOT NULL,
    title VARCHAR(160) NOT NULL,
    estimated_minutes INTEGER,
    intensity VARCHAR(32),
    CONSTRAINT uk_training_program_session UNIQUE (week_id, session_number)
);

CREATE TABLE IF NOT EXISTS training_program_session_item (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES training_program_session(id) ON DELETE CASCADE,
    exercise_id BIGINT REFERENCES exercise(id),
    video_id BIGINT REFERENCES workout_video(id),
    sort_order INTEGER NOT NULL,
    sets_count INTEGER,
    reps_or_duration VARCHAR(80),
    rest_seconds INTEGER,
    instruction TEXT
);

CREATE TABLE IF NOT EXISTS user_program_enrollment (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account(id),
    program_id BIGINT NOT NULL REFERENCES training_program(id) ON DELETE CASCADE,
    current_week INTEGER NOT NULL DEFAULT 1,
    current_session INTEGER NOT NULL DEFAULT 1,
    completed_sessions INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT uk_user_program UNIQUE (user_id, program_id)
);

INSERT INTO exercise (
    name,
    description,
    primary_muscle,
    equipment,
    exercise_type,
    mechanics_type,
    difficulty,
    risk_level
)
SELECT DISTINCT
    LEFT(title, 160),
    description,
    COALESCE(target_body_part, 'FULL_BODY'),
    COALESCE(equipment_requirement, 'NONE'),
    COALESCE(target_goal, 'GENERAL'),
    CASE
        WHEN COALESCE(target_body_part, '') IN ('ARMS', 'SHOULDERS', 'ABS') THEN 'ISOLATION'
        ELSE 'COMPOUND'
    END,
    COALESCE(difficulty, 'BEGINNER'),
    CASE
        WHEN impact_level = 'HIGH' THEN 'HIGH'
        WHEN impact_level = 'MEDIUM' THEN 'MEDIUM'
        ELSE 'LOW'
    END
FROM workout_video
WHERE active = TRUE
ON CONFLICT (name) DO NOTHING;

INSERT INTO exercise_video (exercise_id, video_id)
SELECT e.id, v.id
FROM workout_video v
JOIN exercise e ON e.name = LEFT(v.title, 160)
WHERE v.active = TRUE
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_exercise_primary_muscle ON exercise(primary_muscle);
CREATE INDEX IF NOT EXISTS idx_exercise_equipment ON exercise(equipment);
CREATE INDEX IF NOT EXISTS idx_exercise_type ON exercise(exercise_type);
CREATE INDEX IF NOT EXISTS idx_training_program_user ON training_program(user_id);
CREATE INDEX IF NOT EXISTS idx_training_program_session_week ON training_program_session(week_id);
