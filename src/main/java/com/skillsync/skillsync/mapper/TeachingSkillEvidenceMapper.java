package com.skillsync.skillsync.mapper;

import com.skillsync.skillsync.dto.request.TeachingSkillEvidenceRequest;
import com.skillsync.skillsync.dto.response.TeachingSkillEvidenceResponse;
import com.skillsync.skillsync.entity.TeachingSkillEvidence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TeachingSkillEvidenceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teachingSkill", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TeachingSkillEvidence toEntity(TeachingSkillEvidenceRequest request);

    @Mapping(target = "teachingSkillId", source = "teachingSkill.id")
    TeachingSkillEvidenceResponse toResponse(TeachingSkillEvidence entity);
}
