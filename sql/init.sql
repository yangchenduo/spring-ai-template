-- ============================================================
--  Spring AI Template - Database Initialization
--  Executed automatically on first PostgreSQL container start
-- ============================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Knowledge base table
CREATE TABLE IF NOT EXISTS knowledge_base (
    id            VARCHAR(64) PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    doc_count     INT DEFAULT 0,
    total_chunks  INT DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge documents table
CREATE TABLE IF NOT EXISTS knowledge_doc (
    id            VARCHAR(64) PRIMARY KEY,
    knowledge_id  VARCHAR(64) NOT NULL,
    file_name     VARCHAR(512) NOT NULL,
    doc_type      VARCHAR(32),
    file_size     BIGINT DEFAULT 0,
    status        VARCHAR(32) DEFAULT 'pending',
    chunk_count   INT DEFAULT 0,
    minio_path    VARCHAR(1024),
    error_msg     TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (knowledge_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_doc_kid ON knowledge_doc(knowledge_id);

-- Chat history table (for persistence beyond Redis TTL)
CREATE TABLE IF NOT EXISTS chat_history (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    role            VARCHAR(32) NOT NULL,
    content         TEXT NOT NULL,
    provider        VARCHAR(32),
    model           VARCHAR(128),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_history_cid ON chat_history(conversation_id);

-- Insert a default knowledge base
INSERT INTO knowledge_base (id, name, description)
VALUES ('default', 'Default Knowledge Base', 'Default knowledge base for document uploads')
ON CONFLICT (id) DO NOTHING;
