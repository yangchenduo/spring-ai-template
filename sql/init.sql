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
-- NOTE: for databases initialized before the storage abstraction rename, run:
--   ALTER TABLE knowledge_doc RENAME COLUMN minio_path TO storage_path;
CREATE TABLE IF NOT EXISTS knowledge_doc (
    id            VARCHAR(64) PRIMARY KEY,
    knowledge_id  VARCHAR(64) NOT NULL,
    file_name     VARCHAR(512) NOT NULL,
    doc_type      VARCHAR(32),
    file_size     BIGINT DEFAULT 0,
    status        VARCHAR(32) DEFAULT 'pending',
    chunk_count   INT DEFAULT 0,
    storage_path  VARCHAR(1024),
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

-- ------------------------------------------------------------
-- Vector store (Spring AI PgVectorStore schema)
-- Created here so prod (initialize-schema: false) works out of the box.
-- embedding dimension MUST equal EMBEDDING_DIMENSIONS (default 1024 for
-- BAAI/bge-m3). After changing to a model with another dimension, run
--   DROP TABLE IF EXISTS vector_store;
-- and re-ingest every document (the pipeline is idempotent).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vector_store (
    id        uuid PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(1024)
);

-- Spring AI's default similarity index (HNSW + cosine, matching the
-- index-type / distance-type configured in application-*.yml)
CREATE INDEX IF NOT EXISTS spring_ai_vector_index
    ON vector_store USING hnsw (embedding vector_cosine_ops);

-- Business filters: knowledge-base isolation at query time and cascade
-- delete of a document's chunks (see RagMetadata)
CREATE INDEX IF NOT EXISTS idx_vector_store_knowledge ON vector_store ((metadata->>'knowledgeId'));
CREATE INDEX IF NOT EXISTS idx_vector_store_doc ON vector_store ((metadata->>'docId'));

-- Insert a default knowledge base
INSERT INTO knowledge_base (id, name, description)
VALUES ('default', 'Default Knowledge Base', 'Default knowledge base for document uploads')
ON CONFLICT (id) DO NOTHING;
