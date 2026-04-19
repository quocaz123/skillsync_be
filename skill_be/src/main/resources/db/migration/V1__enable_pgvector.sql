-- ══════════════════════════════════════════════════════════════════
--  V1 – Bật pgvector extension + thêm cột embedding vào bảng skills
--  Chạy thủ công hoặc qua VectorSchemaInitializer (ApplicationRunner)
-- ══════════════════════════════════════════════════════════════════

-- 1. Bật extension pgvector (cần PostgreSQL >= 11 + pgvector cài sẵn)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Thêm cột embedding 1536 chiều (request outputDimensionality)
ALTER TABLE skills
    ADD COLUMN IF NOT EXISTS embedding vector(1536);

-- 3. HNSW index – không yêu cầu tối thiểu số dòng (khác IVFFlat)
--    Phù hợp cho catalog nhỏ (< 10k skills) với latency thấp
CREATE INDEX IF NOT EXISTS idx_skills_embedding_hnsw
    ON skills USING hnsw (embedding vector_cosine_ops);

-- ── Ghi chú ────────────────────────────────────────────────────────
-- gemini-embedding-001 → default 3072, nhưng có thể giảm bằng outputDimensionality
-- Cosine similarity   → 1 − (embedding <=> query_vec)
-- HNSW vs IVFFlat     → HNSW: build chậm hơn nhưng query nhanh hơn
--                        và không cần min-rows khi CREATE INDEX
-- ──────────────────────────────────────────────────────────────────
