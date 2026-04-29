package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.skill.SkillResponse;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.repository.SkillRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    /**
     * Cache thứ tự seed — đọc file JSON 1 lần duy nhất lúc khởi động.
     * Key = skill name, Value = index trong skills.json (để sort đúng thứ tự seed).
     */
    private Map<String, Integer> seedOrderCache;

    @PostConstruct
    void initSeedOrderCache() {
        try {
            ClassPathResource resource = new ClassPathResource("seeds/skills.json");
            try (InputStream inputStream = resource.getInputStream()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> seedSkills = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(inputStream, List.class);

                Map<String, Integer> order = new HashMap<>();
                for (int i = 0; i < seedSkills.size(); i++) {
                    Object name = seedSkills.get(i).get("name");
                    if (name != null) {
                        order.put(name.toString(), i);
                    }
                }
                seedOrderCache = order;
                log.info("[SkillService] Đã cache thứ tự {} skill từ seeds/skills.json", order.size());
            }
        } catch (Exception e) {
            log.error("[SkillService] Không thể đọc seeds/skills.json: {}", e.getMessage());
            seedOrderCache = new HashMap<>(); // fallback: empty map, không crash
        }
    }

    public List<SkillResponse> getAll(SkillCategory category) {
        var skills = (category != null)
                ? skillRepository.findByCategory(category)
                : skillRepository.findAll();

        return skills.stream()
                // Skill có trong seed → sort theo thứ tự seed; skill ngoài seed → đẩy xuống cuối
                .sorted(Comparator.comparingInt(skill ->
                        seedOrderCache.getOrDefault(skill.getName(), Integer.MAX_VALUE)))
                .map(s -> SkillResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .category(s.getCategory())
                        .icon(s.getIcon())
                        .build())
                .toList();
    }
}
