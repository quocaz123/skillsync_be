package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.response.skill.SkillResponse;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.service.SkillService;
import lombok.RequiredArgsConstructor;
import com.skillsync.skillsync.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ApiResponse<List<SkillResponse>> getAll(
            @RequestParam(required = false) SkillCategory category
    ) {
        return ApiResponse.success(skillService.getAll(category));
    }
}
