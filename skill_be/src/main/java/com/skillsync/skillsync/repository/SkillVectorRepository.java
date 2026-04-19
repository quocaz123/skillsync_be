package com.skillsync.skillsync.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Repository xử lý các thao tác vector (pgvector) cho bảng skills.
 *
 * <p>Dùng {@link JdbcTemplate} với native SQL thay vì JPA vì:
 * <ul>
 *   <li>Hibernate không có built-in type mapping cho pgvector
 *   <li>Tránh thêm dependency pgvector-java (giữ pom.xml nhỏ gọn)
 *   <li>Native SQL cho phép dùng {@code ::vector} cast và {@code <=>} operator trực tiếp
 * </ul>
 *
 * <p>Operator pgvector:
 * <pre>
 *   {@code <=>}  cosine distance     (giá trị nhỏ = gần nhau)
 *   {@code <->}  L2 (Euclidean) distance
 *   {@code <#>}  inner product (negative)
 * </pre>
 * Similarity = 1 − cosine_distance
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SkillVectorRepository {

    private final JdbcTemplate jdbc;

    // ── Write ─────────────────────────────────────────────────────────────

    /**
     * Lưu / cập nhật embedding của một skill.
     *
     * @param skillId   UUID của skill
     * @param embedding float[] (hiện tại 3072 chiều từ gemini-embedding-001)
     */
    public void saveEmbedding(UUID skillId, float[] embedding) {
        if (skillId == null || embedding == null || embedding.length == 0) return;

        String vectorStr = toVectorLiteral(embedding);
        int updated = jdbc.update(
                "UPDATE skills SET embedding = ?::vector WHERE id = ?::uuid",
                vectorStr, skillId.toString()
        );
        if (updated == 0) {
            log.warn("[SkillVector] Không tìm thấy skill id={} để lưu embedding.", skillId);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────

    /**
     * Tìm top-N skill tương tự nhất theo cosine similarity.
     *
     * @param queryEmbedding float[] — vector đã embed từ query
     * @param topN           số lượng kết quả muốn lấy
     * @return danh sách {@link SimilarSkill} sắp xếp giảm dần theo similarity
     */
    public List<SimilarSkill> findSimilarSkills(float[] queryEmbedding, int topN) {
        if (queryEmbedding == null || queryEmbedding.length == 0 || topN <= 0) {
            return Collections.emptyList();
        }

        String vectorStr = toVectorLiteral(queryEmbedding);

        String sql = """
                SELECT id,
                       name,
                       category,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM   skills
                WHERE  embedding IS NOT NULL
                ORDER  BY embedding <=> ?::vector
                LIMIT  ?
                """;

        try {
            return jdbc.query(sql,
                    (rs, rowNum) -> new SimilarSkill(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getDouble("similarity")
                    ),
                    vectorStr, vectorStr, topN);
        } catch (Exception e) {
            log.error("[SkillVector] Lỗi tìm kiếm vector: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Kiểm tra skill đã có embedding chưa.
     *
     * @param skillId UUID của skill cần kiểm tra
     */
    public boolean hasEmbedding(UUID skillId) {
        if (skillId == null) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM skills WHERE id = ?::uuid AND embedding IS NOT NULL",
                Integer.class,
                skillId.toString()
        );
        return count != null && count > 0;
    }

    /**
     * Lấy số lượng skill đã có embedding.
     * Dùng để log tiến độ trong SkillDataSeeder.
     */
    public int countEmbedded() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM skills WHERE embedding IS NOT NULL",
                Integer.class
        );
        return count != null ? count : 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Chuyển float[] sang pgvector literal "[0.1,0.2,...,0.768]".
     */
    private String toVectorLiteral(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    // ── Result Record ─────────────────────────────────────────────────────

    /**
     * Kết quả tìm kiếm vector similarity.
     *
     * @param id         UUID skill
     * @param name       tên skill (ví dụ: "Machine Learning")
     * @param category   enum name (ví dụ: "DATA")
     * @param similarity giá trị cosine similarity [0, 1] — cao hơn = gần hơn
     */
    public record SimilarSkill(
            UUID id,
            String name,
            String category,
            double similarity
    ) {}
}
