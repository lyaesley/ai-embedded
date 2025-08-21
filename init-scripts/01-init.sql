-- Spring AI Vector Store 테이블 생성
CREATE TABLE IF NOT EXISTS vector_store (
                                            id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
                                            content text NOT NULL,
                                            metadata jsonb,  -- json을 jsonb로 변경
                                            embedding vector(1536) NOT NULL -- OpenAI text-embedding-3-small 차원
);


-- 벡터 유사도 검색을 위한 HNSW 인덱스
CREATE INDEX IF NOT EXISTS vector_store_embedding_hnsw_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 메타데이터 검색을 위한 GIN 인덱스
CREATE INDEX IF NOT EXISTS vector_store_metadata_gin_idx
    ON vector_store USING gin (metadata jsonb_path_ops);

-- 전문 검색을 위한 GIN 인덱스
CREATE INDEX IF NOT EXISTS vector_store_content_gin_idx
    ON vector_store USING gin (to_tsvector('english', content));

-- 테이블 정보 확인
\d vector_store

-- 인덱스 확인
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'vector_store';