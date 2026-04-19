package ai.dto.response;

import java.util.List;

public record ChatResponse(
        String message,
        boolean isSearchResult,
        List<MentorMatchDto> mentors
) {}
