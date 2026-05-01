CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_name VARCHAR(160),
    language VARCHAR(16) NOT NULL DEFAULT 'zh_CN',
    topic VARCHAR(64),
    tags VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES knowledge_document(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    metadata_json TEXT,
    indexed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_knowledge_chunk_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_document_status ON knowledge_document(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_document_language ON knowledge_document(language);
CREATE INDEX IF NOT EXISTS idx_knowledge_document_topic ON knowledge_document(topic);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_document_id ON knowledge_chunk(document_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_indexed ON knowledge_chunk(indexed);
