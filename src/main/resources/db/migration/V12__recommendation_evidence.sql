ALTER TABLE recommendation_history
    ADD COLUMN IF NOT EXISTS evidence_json TEXT;
