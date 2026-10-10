CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_store (
                              id        UUID PRIMARY KEY,
                              content   TEXT NOT NULL,
                              metadata  JSON,
                              embedding vector(768)
);

CREATE INDEX vector_store_embedding_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);