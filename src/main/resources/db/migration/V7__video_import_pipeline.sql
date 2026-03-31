CREATE TABLE import_source (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    external_id VARCHAR(128) NOT NULL,
    channel_name VARCHAR(128),
    default_goal VARCHAR(32),
    default_equipment VARCHAR(32),
    default_posture VARCHAR(32),
    auto_approve_confident BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_imported_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE imported_video (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES import_source(id),
    source_video_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(255),
    video_url VARCHAR(255) NOT NULL,
    channel_name VARCHAR(128),
    published_at TIMESTAMP,
    import_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    suggested_goal VARCHAR(32),
    suggested_equipment VARCHAR(32),
    suggested_posture VARCHAR(32),
    suggested_target_area VARCHAR(32),
    suggested_difficulty VARCHAR(32),
    suggested_impact_level VARCHAR(32),
    safety_flags VARCHAR(255),
    confidence_score NUMERIC(4,2),
    review_note VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source_id, source_video_id)
);

CREATE INDEX idx_imported_video_status ON imported_video(import_status);
CREATE INDEX idx_import_source_enabled ON import_source(enabled);

