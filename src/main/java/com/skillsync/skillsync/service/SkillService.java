package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.skill.SkillResponse;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    public List<SkillResponse> getAll(SkillCategory category) {
        var skills = (category != null)
                ? skillRepository.findByCategory(category)
                : skillRepository.findAll();

        Map<String, Integer> seedOrder = loadSeedSkillOrder();

        return skills.stream()
                .filter(skill -> seedOrder.containsKey(skill.getName()))
                .sorted(Comparator.comparingInt(skill -> seedOrder.get(skill.getName())))
                .map(s -> SkillResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .category(s.getCategory())
                        .icon(s.getIcon())
                        .build())
                .toList();
    }

    private Map<String, Integer> loadSeedSkillOrder() {
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
                return order;
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc danh sách skills seed", e);
        }
    }
}
