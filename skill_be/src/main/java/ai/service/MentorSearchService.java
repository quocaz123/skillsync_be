package ai.service;

import ai.config.AiConfigHolder;
import ai.dto.response.MentorMatchDto;
import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.VerificationStatus;
import com.skillsync.skillsync.repository.SkillVectorRepository;
import com.skillsync.skillsync.repository.SkillVectorRepository.SimilarSkill;
import com.skillsync.skillsync.repository.UserTeachingSkillRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tìm kiếm mentor theo cơ chế Hybrid = Alias + Vector.
 *
 * <p><b>Luồng tìm kiếm:</b>
 * <ol>
 *   <li><b>Alias search</b>: JPQL exact/lower-case match tên skill trong DB
 *       (nhanh, chính xác, là nguồn primary)
 *   <li><b>Vector expansion</b> (tuỳ chọn): nếu {@code skillsync.ai.vector-search-enabled=true}
 *       VÀ số kết quả alias < threshold → embed query → tìm skill tương tự → mở rộng tìm kiếm
 *       <br>Ví dụ: "ML" → vector tìm "Machine Learning" → tìm mentor dạy Machine Learning
 * </ol>
 *
 * <p><b>Scoring:</b>
 * <pre>
 *   exact name match    +50
 *   partial name match  +30
 *   vector-expanded     +20  (penalty vì kém chắc chắn hơn exact)
 *   level exact match   +25
 *   level nearby        +10
 *   experienceDesc ≠ "" +10
 *   teachingStyle ≠ ""  + 8
 *   creditsPerHour ≠ null+2
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MentorSearchService {

    final UserTeachingSkillRepository teachingSkillRepository;
    final SkillVectorRepository       skillVectorRepository;
    final SkillEmbeddingService       embeddingService;
    final AiConfigHolder              configHolder;

    /** Ngưỡng cosine similarity — tăng lên 0.80 để chỉ lấy skill thực sự tương tự. */
    @Value("${skillsync.ai.vector-search-threshold:0.80}")
    double vectorSimilarityThresholdDefault;

    /**
     * Minimum score từ skill match để mentor được đưa vào kết quả.
     * Loại bỏ mentor có skill hoàn toàn không liên quan (Java mentor cho React query).
     */
    private static final int MIN_SKILL_SCORE = 15;

    /**
     * Built-in alias map: safety net khi AI extract sai tên skill.
     * Key = tên AI có thể trả về (lowercase), Value = tên chuẩn trong DB.
     */
    private static final Map<String, String> SKILL_ALIASES = Map.ofEntries(
            // ── React ecosystem ───────────────────────────────────────────
            Map.entry("reactjs",           "react"),
            Map.entry("react.js",          "react"),
            Map.entry("react js",          "react"),
            Map.entry("nextjs",            "react"),
            Map.entry("next.js",           "react"),
            Map.entry("next js",           "react"),
            // ── Vue ───────────────────────────────────────────────────────
            Map.entry("vuejs",             "vue.js"),
            Map.entry("vue js",            "vue.js"),
            Map.entry("vue",               "vue.js"),
            // ── Node.js ───────────────────────────────────────────────────
            Map.entry("nodejs",            "node.js"),
            Map.entry("node js",           "node.js"),
            Map.entry("node",              "node.js"),
            // ── Angular → JS (không có skill Angular riêng) ───────────────
            Map.entry("angularjs",         "javascript"),
            Map.entry("angular",           "javascript"),
            Map.entry("html",              "javascript"),
            Map.entry("css",               "javascript"),
            Map.entry("web design",        "react"),
            Map.entry("thiết kế web",      "react"),
            // ── TypeScript ───────────────────────────────────────────────
            Map.entry("ts",                "typescript"),
            // ── Spring Boot ───────────────────────────────────────────────
            Map.entry("spring",            "spring boot"),
            Map.entry("spring mvc",        "spring boot"),
            Map.entry("spring framework",  "spring boot"),
            // ── Docker / DevOps ───────────────────────────────────────────
            Map.entry("devops",            "docker"),
            Map.entry("container",         "docker"),
            Map.entry("kubernetes",        "docker"),
            Map.entry("k8s",               "docker"),
            // ── Flutter / Mobile ──────────────────────────────────────────
            Map.entry("mobile",            "flutter"),
            Map.entry("mobile app",        "flutter"),
            Map.entry("react native",      "flutter"),
            // ── Data & AI ─────────────────────────────────────────────────
            Map.entry("ml",                "machine learning"),
            Map.entry("ai",                "machine learning"),
            Map.entry("deep learning",     "machine learning"),
            Map.entry("data science",      "machine learning"),
            Map.entry("data",              "data analysis"),
            Map.entry("analytics",         "data analysis"),
            // ── Design ────────────────────────────────────────────────────
            Map.entry("ux",                "ui/ux design"),
            Map.entry("ux design",         "ui/ux design"),
            Map.entry("ui design",         "ui/ux design"),
            Map.entry("uiux",              "ui/ux design"),
            Map.entry("ui/ux",             "ui/ux design"),
            Map.entry("ps",                "photoshop"),
            Map.entry("adobe photoshop",   "photoshop"),
            Map.entry("adobe illustrator", "illustrator"),
            // ── Finance ───────────────────────────────────────────────────
            Map.entry("chứng khoán",       "đầu tư chứng khoán"),
            Map.entry("cổ phiếu",          "đầu tư chứng khoán"),
            Map.entry("đầu tư",            "đầu tư chứng khoán"),
            Map.entry("tài chính",         "tài chính cá nhân"),
            // ── Languages ─────────────────────────────────────────────────
            Map.entry("english",           "tiếng anh"),
            Map.entry("ielts",             "tiếng anh"),
            Map.entry("toeic",             "tiếng anh"),
            Map.entry("japanese",          "tiếng nhật"),
            Map.entry("jlpt",              "tiếng nhật"),
            Map.entry("nhật",              "tiếng nhật"),
            Map.entry("chinese",           "tiếng trung"),
            Map.entry("hsk",               "tiếng trung"),
            Map.entry("trung",             "tiếng trung"),
            Map.entry("korean",            "tiếng hàn"),
            Map.entry("topik",             "tiếng hàn"),
            Map.entry("hàn",               "tiếng hàn"),
            // ── Creative ─────────────────────────────────────────────────
            Map.entry("đàn piano",         "piano"),
            Map.entry("âm nhạc",           "piano"),
            Map.entry("đàn guitar",        "guitar"),
            Map.entry("guitar acoustic",   "guitar"),
            Map.entry("vẽ",                "vẽ tranh"),
            Map.entry("painting",          "vẽ tranh"),
            Map.entry("photography",       "chụp ảnh"),
            Map.entry("nhiếp ảnh",         "chụp ảnh"),
            Map.entry("video editing",     "làm video"),
            Map.entry("dựng phim",         "làm video"),
            Map.entry("quay phim",         "làm video")
    );


    // ── Public API ────────────────────────────────────────────────────────

    public List<MentorMatchDto> findMentors(List<String> skills, String level) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }

        // Normalize + áp dụng alias map trước khi query
        List<String> normalizedSkills = applyAliases(normalize(skills));
        if (normalizedSkills.isEmpty()) {
            return List.of();
        }
        log.debug("[MentorSearch] Skills sau alias '{}' → normalized '{}'", skills, normalizedSkills);

        String normalizedLevel = normalizeLevel(level);

        // ── Phase 1: Alias search ─────────────────────────────────────────
        List<UserTeachingSkill> aliasResults = teachingSkillRepository.findApprovedBySkillNames(
                normalizedSkills, VerificationStatus.APPROVED);

        log.debug("[MentorSearch] Alias '{}' → {} kết quả", normalizedSkills, aliasResults.size());

        // ── Phase 2: Vector expansion (nếu cần) ──────────────────────────
        Set<String> vectorExpandedNames = new HashSet<>();
        if (shouldExpandWithVector(aliasResults)) {
            vectorExpandedNames = expandViaVector(normalizedSkills, normalizedLevel);
            log.debug("[MentorSearch] Vector expansion → thêm skills: {}", vectorExpandedNames);
        }

        List<UserTeachingSkill> vectorResults = Collections.emptyList();
        if (!vectorExpandedNames.isEmpty()) {
            vectorResults = teachingSkillRepository.findApprovedBySkillNames(
                    new ArrayList<>(vectorExpandedNames), VerificationStatus.APPROVED);
            log.debug("[MentorSearch] Vector results: {} kết quả", vectorResults.size());
        }

        // ── Phase 3: Merge + Score + Dedup ───────────────────────────────
        Map<UUID, ScoredMentor> mentorMap = new LinkedHashMap<>();

        mergeScoredResults(mentorMap, aliasResults, normalizedSkills, normalizedLevel, false);
        mergeScoredResults(mentorMap, vectorResults,  normalizedSkills, normalizedLevel, true);

        return mentorMap.values().stream()
                .sorted(Comparator.comparingInt(ScoredMentor::score).reversed())
                .limit(5)
                .map(sm -> toDto(sm.uts()))
                .toList();
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private boolean shouldExpandWithVector(List<UserTeachingSkill> aliasResults) {
        return configHolder.isVectorSearchEnabled()
                && embeddingService.probe()
                && aliasResults.size() < configHolder.getVectorMinPrimaryResults();
    }

    /**
     * Với mỗi query skill, embed + tìm skill tương tự trong DB,
     * lọc theo threshold và trả về tập tên skill mới (chưa có trong alias).
     */
    private Set<String> expandViaVector(List<String> querySkills, String level) {
        Set<String> expanded = new LinkedHashSet<>();

        for (String querySkill : querySkills) {
            try {
                String text = embeddingService.buildSkillText(querySkill, null);
                float[] vec = embeddingService.embed(text);
                if (vec == null) continue;

                skillVectorRepository.findSimilarSkills(vec, 4).stream()
                        .filter(s -> s.similarity() >= configHolder.getVectorSearchThreshold())
                        .map(s -> s.name().toLowerCase())
                        .filter(n -> !querySkills.contains(n))
                        .forEach(expanded::add);

            } catch (Exception e) {
                log.warn("[MentorSearch] Vector expand thất bại cho '{}': {}", querySkill, e.getMessage());
            }
        }
        return expanded;
    }

    /** Gộp kết quả vào map, bỏ qua mentor có skill score = 0 (không liên quan). */
    private void mergeScoredResults(
            Map<UUID, ScoredMentor> mentorMap,
            List<UserTeachingSkill> results,
            List<String> targetSkills,
            String targetLevel,
            boolean isVectorExpanded
    ) {
        for (UserTeachingSkill uts : results) {
            if (uts.getUser() == null || uts.getUser().getId() == null) continue;

            UUID mentorId = uts.getUser().getId();
            int sc = score(uts, targetSkills, targetLevel, isVectorExpanded);
            if (sc == 0) continue; // ⛔ bỏ qua mentor không liên quan

            mentorMap.merge(
                    mentorId,
                    new ScoredMentor(uts, sc),
                    (existing, incoming) -> incoming.score() > existing.score() ? incoming : existing
            );
        }
    }

    // ── Scoring ───────────────────────────────────────────────────────────

    private int score(
            UserTeachingSkill uts,
            List<String> targetSkills,
            String targetLevel,
            boolean isVectorExpanded
    ) {
        int skillScore = 0;

        // Skill name match
        String skillName = uts.getSkill() != null && uts.getSkill().getName() != null
                ? uts.getSkill().getName().toLowerCase()
                : "";

        if (targetSkills.contains(skillName)) {
            skillScore += isVectorExpanded ? 20 : 50;
        } else {
            for (String s : targetSkills) {
                if (skillName.contains(s) || s.contains(skillName)) {
                    skillScore += isVectorExpanded ? 15 : 30;
                    break;
                }
            }
        }

        // ⛔ Gate: loại mentor không có skill liên quan gì
        if (skillScore < MIN_SKILL_SCORE) {
            return 0;
        }

        int score = skillScore;

        // Level match
        String mentorLevel = uts.getLevel() != null ? uts.getLevel().name() : "";
        if (mentorLevel.equalsIgnoreCase(targetLevel)) {
            score += 25;
        } else if (isNearbyLevel(mentorLevel, targetLevel)) {
            score += 10;
        }

        // Richness bonus
        if (uts.getExperienceDesc() != null && !uts.getExperienceDesc().isBlank()) score += 10;
        if (uts.getTeachingStyle()  != null && !uts.getTeachingStyle().isBlank())  score += 8;
        if (uts.getCreditsPerHour() != null)                                        score += 2;

        return score;
    }

    private boolean isNearbyLevel(String a, String b) {
        List<String> levels = List.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
        int ia = levels.indexOf(a.toUpperCase());
        int ib = levels.indexOf(b.toUpperCase());
        return ia >= 0 && ib >= 0 && Math.abs(ia - ib) == 1;
    }

    // ── DTO mapping ───────────────────────────────────────────────────────

    private MentorMatchDto toDto(UserTeachingSkill uts) {
        return new MentorMatchDto(
                uts.getUser().getId(),
                uts.getUser().getFullName(),
                uts.getUser().getAvatarUrl(),
                uts.getSkill()  != null ? uts.getSkill().getName()   : null,
                uts.getLevel()  != null ? uts.getLevel().name()      : null,
                uts.getExperienceDesc(),
                uts.getTeachingStyle(),
                uts.getCreditsPerHour(),
                null,
                null
        );
    }

    // ── Utils ─────────────────────────────────────────────────────────────

    private List<String> normalize(List<String> skills) {
        return skills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    /**
     * Áp dụng alias map: nếu skill AI trả về là alias thì map sang tên chuẩn.
     * Giữ lại skill không có trong alias map (có thể đã đúng tên DB).
     */
    private List<String> applyAliases(List<String> normalizedSkills) {
        return normalizedSkills.stream()
                .map(s -> SKILL_ALIASES.getOrDefault(s, s))
                .distinct()
                .toList();
    }

    private String normalizeLevel(String level) {
        if (level == null) return "BEGINNER";
        return switch (level.trim().toUpperCase()) {
            case "BEGINNER", "INTERMEDIATE", "ADVANCED" -> level.trim().toUpperCase();
            default -> "BEGINNER";
        };
    }

    // ── Inner record ──────────────────────────────────────────────────────

    private record ScoredMentor(UserTeachingSkill uts, int score) {}
}
