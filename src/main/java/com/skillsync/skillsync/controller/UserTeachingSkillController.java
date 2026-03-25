package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.CreateTeachingSkillRequest;
import com.skillsync.skillsync.dto.response.TeachingSkillResponse;
import com.skillsync.skillsync.service.UserTeachingSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teaching-skills")
@RequiredArgsConstructor
public class UserTeachingSkillController {

    private final UserTeachingSkillService service;

    @GetMapping("/me")
    public List<TeachingSkillResponse> getMyTeachingSkills() {
        return service.getMyTeachingSkills();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeachingSkillResponse create(@RequestBody CreateTeachingSkillRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
