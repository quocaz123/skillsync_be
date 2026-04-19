package com.skillsync.skillsync.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * ApplicationRunner @Order(1) — chạy trước SkillDataSeeder.
 *
 * Nhiệm vụ:
 *   1. Bật pgvector extension (CREATE EXTENSION IF NOT EXISTS vector)
 *   2. Thêm cột embedding vector(1536) vào bảng skills nếu chưa có
 *   3. Tạo HNSW index cho tìm kiếm cosine similarity nhanh hơn
 *
 * Lý do dùng ApplicationRunner thay vì Flyway:
 *   - Project đang dùng ddl-auto: update → Hibernate quản lý entity schema
 *   - Chỉ cần migration 1 lần cho pgvector, không cần cả framework migration
 *   - ApplicationRunner chạy sau Hibernate đã tạo/update bảng skills
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class VectorSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[pgvector] Khởi tạo pgvector extension và schema...");

        try {
            // Step 1 – Bật pgvector extension
            jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("[pgvector] Extension 'vector' đã bật.");

            // Step 2 – Thêm cột embedding nếu chưa có (request outputDimensionality=1536)
            jdbc.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (
                            SELECT 1
                            FROM   information_schema.columns
                            WHERE  table_name  = 'skills'
                            AND    column_name = 'embedding'
                        ) THEN
                            ALTER TABLE skills ADD COLUMN embedding vector(1536);
                            RAISE NOTICE 'Đã thêm cột skills.embedding vector(1536)';
                        ELSE
                            -- Nếu cột đã tồn tại nhưng sai chiều, ép kiểu sang vector(1536)
                            -- (dữ liệu cũ sẽ bị NULL nếu không cast được — nên bảng đang NULL là an toàn)
                            BEGIN
                                ALTER TABLE skills
                                    ALTER COLUMN embedding TYPE vector(1536)
                                    USING embedding::vector(1536);
                                RAISE NOTICE 'Đã cập nhật cột skills.embedding sang vector(1536)';
                            EXCEPTION WHEN others THEN
                                RAISE NOTICE 'Không thể ALTER TYPE skills.embedding sang vector(1536) (bỏ qua).';
                            END;
                        END IF;
                    END;
                    $$
                    """);
            log.info("[pgvector] Cột skills.embedding sẵn sàng.");

            // Step 3 – HNSW index (không yêu cầu min-rows, phù hợp catalog nhỏ)
            jdbc.execute("""
                    CREATE INDEX IF NOT EXISTS idx_skills_embedding_hnsw
                        ON skills USING hnsw (embedding vector_cosine_ops)
                    """);
            log.info("[pgvector] HNSW index sẵn sàng.");

        } catch (Exception ex) {
            // Warn instead of crash: pgvector có thể chưa cài trên môi trường dev
            log.warn("[pgvector] Khởi tạo thất bại — pgvector chưa được cài? Lỗi: {}",
                    ex.getMessage());
        }
    }
}
