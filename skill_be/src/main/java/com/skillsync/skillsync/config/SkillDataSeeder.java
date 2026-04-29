package com.skillsync.skillsync.config;

import ai.service.SkillEmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.repository.SkillRepository;
import com.skillsync.skillsync.repository.SkillVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ApplicationRunner @Order(2) — chạy SAU VectorSchemaInitializer (@Order 1).
 *
 * <p>Nhiệm vụ:
 * <ol>
 *   <li>Đọc {@code resources/seeds/skills.json}
 *   <li>Insert skill nếu chưa tồn tại (theo {@code name})
 *   <li>Với mỗi skill chưa có embedding → gọi Gemini text-embedding-004 → lưu vào DB
 * </ol>
 *
 * <p>Rate limit free tier Gemini embedding API: ~100 req/min.
 * Vì vậy có sleep 700ms giữa các lần embed để tránh 429.
 * Khi chạy lại lần 2 trở đi, tất cả skills đã có embedding → không gọi API nữa.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class SkillDataSeeder implements ApplicationRunner {

    private static final long EMBED_RATE_LIMIT_DELAY_MS = 700L;

    private final SkillRepository        skillRepository;
    private final SkillVectorRepository  skillVectorRepository;
    private final SkillEmbeddingService  embeddingService;
    private final ObjectMapper           objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("[SkillSeeder] ══ Bắt đầu seed skills ══");

        List<Map<String, String>> seedData = loadSeedFile();
        if (seedData == null || seedData.isEmpty()) {
            log.warn("[SkillSeeder] skills.json rỗng hoặc không đọc được, bỏ qua.");
            return;
        }

        // Kiểm tra xem có skill nào trong JSON chưa có trong DB không
        long jsonCount   = seedData.stream().filter(m -> m.get("name") != null && !m.get("name").isBlank()).count();
        long dbCount     = skillRepository.count();
        int embeddedSkills = skillVectorRepository.countEmbedded();

        boolean allSkillsExist = dbCount >= jsonCount;
        boolean allEmbedded    = embeddedSkills >= dbCount;

        if (allSkillsExist && allEmbedded) {
            log.info("[SkillSeeder] Bỏ qua: đã có {}/{} skills với đủ embeddings.", embeddedSkills, dbCount);
            return;
        }

        if (!allSkillsExist) {
            log.info("[SkillSeeder] Phát hiện {} skill mới trong JSON (DB hiện có {}), sẽ insert thêm.",
                    jsonCount - dbCount, dbCount);
        }

        // Probe một lần duy nhất trước khi loop — tránh 20×2 lần gọi API khi không có model nào
        boolean canEmbed = embeddingService.probe();
        if (!canEmbed) {
            log.warn("[SkillSeeder] Embedding không khả dụng — chỉ seed tên skill, bỏ qua embedding.");
        }

        int inserted = 0;
        int embedded = 0;
        int skipped  = 0;

        for (Map<String, String> item : seedData) {
            String name        = item.get("name");
            String categoryStr = item.get("category");
            String icon        = item.get("icon");

            if (name == null || name.isBlank()) {
                skipped++;
                continue;
            }

            // ── 1. Upsert skill ──────────────────────────────────────────
            Skill skill = upsertSkill(name, categoryStr, icon);
            if (skill == null) {
                skipped++;
                continue;
            }
            if (!skillRepository.existsByName(name) || skill.getId() == null) {
                inserted++;
            }

            // ── 2. Generate + save embedding nếu chưa có ─────────────────
            if (!skillVectorRepository.hasEmbedding(skill.getId())) {
                if (!canEmbed) {
                    continue; // đã log ở trên, không spam thêm
                }

                String embText = embeddingService.buildSkillText(
                        skill.getName(),
                        skill.getCategory() != null ? skill.getCategory().name() : "OTHER"
                );

                float[] vec = embeddingService.embed(embText);
                if (vec != null) {
                    skillVectorRepository.saveEmbedding(skill.getId(), vec);
                    embedded++;
                    log.debug("[SkillSeeder] ✓ Embed '{}' ({} chiều)", name, vec.length);

                    // Chờ giữa các lần gọi API để tránh 429 (free tier 100 req/min)
                    Thread.sleep(EMBED_RATE_LIMIT_DELAY_MS);
                } else {
                    log.warn("[SkillSeeder] Không embed được skill '{}'.", name);
                }
            }
        }

        log.info("[SkillSeeder] ══ Kết quả: +{} skill mới | +{} embeddings | {} bỏ qua ══",
                inserted, embedded, skipped);
        log.info("[SkillSeeder] Tổng embedding hiện tại: {}/{} skills",
                skillVectorRepository.countEmbedded(), skillRepository.count());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Tìm skill theo tên; nếu chưa có thì tạo mới.
     * Trả về null nếu tên hợp lệ nhưng có lỗi DB.
     */
    private Skill upsertSkill(String name, String categoryStr, String icon) {
        Optional<Skill> existing = skillRepository.findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }

        SkillCategory category;
        try {
            category = SkillCategory.valueOf(categoryStr);
        } catch (Exception e) {
            log.warn("[SkillSeeder] Category '{}' không hợp lệ cho skill '{}', dùng OTHER.",
                    categoryStr, name);
            category = SkillCategory.OTHER;
        }

        Skill newSkill = Skill.builder()
                .name(name)
                .category(category)
                .icon(icon)
                .build();

        return skillRepository.save(newSkill);
    }

    private List<Map<String, String>> loadSeedFile() {
        try {
            ClassPathResource resource = new ClassPathResource("seeds/skills.json");
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.error("[SkillSeeder] Không đọc được seeds/skills.json: {}", e.getMessage());
            return null;
        }
    }
}
