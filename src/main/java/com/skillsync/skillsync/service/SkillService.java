package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.skill.SkillResponse;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    public List<SkillResponse> getAll(SkillCategory category) {
        var skills = (category != null)
                ? skillRepository.findByCategory(category)
                : skillRepository.findAll();

        return skills.stream()
                .map(s -> SkillResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .category(s.getCategory())
                        .icon(s.getIcon())
                        .build())
                .toList();
    }
}
