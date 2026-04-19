package ai.service;

import ai.config.AiConfigHolder;
import ai.dto.response.ChatResponse;
import ai.dto.response.MentorMatchDto;
import ai.memory.AiSessionRepository;
import ai.memory.AiSessionState;
import ai.memory.ChatTurn;
import ai.memory.MemoryPromptBuilder;
import ai.memory.SessionSummaryService;
import ai.prompt.SystemPromptTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Xử lý hội thoại AI với bộ nhớ phiên lưu trên Redis.
 *
 * <p>Luồng chính:
 * <pre>
 * processMessage(sessionId, userMessage)
 *   ├─ AiSessionRepository.getOrCreate()           ← Redis GET
 *   ├─ Rate-limit guard (lastRequestAt)
 *   ├─ MemoryPromptBuilder.buildTurns()             ← summary + history + user msg
 *   ├─ GroqChatService.generateWithHistory()        ← Groq Cloud LLM call
 *   ├─ Append (user, assistant) → state.recentTurns
 *   ├─ [turns >= summaryTrigger] → SessionSummaryService.refreshSummary()
 *   ├─ AiSessionRepository.save()                  ← Redis SET với TTL
 *   └─ Parse ##SEARCH## → MentorSearchService → ChatResponse
 *       └─ AiSessionRepository.delete()            ← Redis DEL sau khi match
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationalRagService {

    // ── Dependencies ───────────────────────────────────────────────────────
    GroqChatService        groqChatService;
    MentorSearchService    mentorSearchService;
    ObjectMapper           objectMapper;
    AiSessionRepository    sessionRepository;
    MemoryPromptBuilder    promptBuilder;
    SessionSummaryService  summaryService;
    AiConfigHolder         configHolder;

    // ── Constants ──────────────────────────────────────────────────────────
    static final long MIN_INTERVAL_BETWEEN_MESSAGES_MS = 2000L;

    // ── Public API ─────────────────────────────────────────────────────────

    public ChatResponse processMessage(String sessionId, String userMessage) {
        if (sessionId == null || sessionId.isBlank()) {
            return new ChatResponse("Thiếu sessionId.", false, null);
        }
        if (userMessage == null || userMessage.isBlank()) {
            return new ChatResponse("Thiếu message.", false, null);
        }

        // 1. Load session từ Redis
        AiSessionState state = sessionRepository.getOrCreate(sessionId);
        long now = System.currentTimeMillis();

        // 2. Rate-limit guard
        if (now - state.getLastRequestAt() < MIN_INTERVAL_BETWEEN_MESSAGES_MS) {
            return new ChatResponse(
                    "Bạn gửi hơi nhanh một chút. Chờ khoảng 2 giây rồi nhắn tiếp nhé!",
                    false,
                    null
            );
        }
        state.setLastRequestAt(now);

        // 3. Build prompt từ memory
        List<GroqChatService.Turn> turns = promptBuilder.buildTurns(state, userMessage);

        try {
            // 4. Gọi LLM
            String aiRawResponse = groqChatService.generateWithHistory(
                    SystemPromptTemplate.MENTOR_MATCHER_PROMPT,
                    turns
            );

            // 5. Ghi lượt hội thoại vào state
            state.getRecentTurns().add(ChatTurn.user(userMessage));
            state.getRecentTurns().add(ChatTurn.assistant(aiRawResponse));

            // 6. Trigger tóm tắt nếu đủ N lượt (runtime-tunable từ AiConfigHolder)
            int triggerTurns = configHolder.getSessionSummaryTrigger();
            if (state.getRecentTurns().size() >= triggerTurns) {
                log.debug("Session {} reached summaryTrigger ({}), refreshing summary...",
                        sessionId, triggerTurns);
                String newSummary = summaryService.refreshSummary(
                        state.getSummary(),
                        state.getRecentTurns()
                );
                state.setSummary(newSummary);
                state.setRecentTurns(new ArrayList<>()); // reset sau khi tóm tắt
            }

            // 7. Lưu state vào Redis
            sessionRepository.save(sessionId, state);

            // 8. Kiểm tra trigger tìm mentor
            if (aiRawResponse.contains("##SEARCH##")) {
                return handleSearchTrigger(aiRawResponse, sessionId);
            }

            return new ChatResponse(aiRawResponse, false, null);

        } catch (Exception e) {
            log.error("AI processing error for session {}: {}", sessionId, e.getMessage(), e);
            return new ChatResponse(
                    "Xin lỗi, mình gặp sự cố kỹ thuật. Bạn thử lại nhé!",
                    false,
                    null
            );
        }
    }

    public void clearSession(String sessionId) {
        sessionRepository.delete(sessionId);
    }

    // ── Internal: Search trigger ───────────────────────────────────────────

    ChatResponse handleSearchTrigger(String aiResponse, String sessionId) {
        try {
            String jsonPart = extractSearchJson(aiResponse);

            Map<String, Object> extracted = objectMapper.readValue(
                    jsonPart,
                    new TypeReference<>() {}
            );

            @SuppressWarnings("unchecked")
            List<Object> rawSkills = (List<Object>) extracted.get("skills");
            if (rawSkills == null) rawSkills = List.of();

            List<String> skills = rawSkills.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();

            String level   = Objects.toString(extracted.getOrDefault("level",   "BEGINNER"), "BEGINNER");
            String goal    = Objects.toString(extracted.getOrDefault("goal",    ""),          "");
            String summary = Objects.toString(
                    extracted.getOrDefault("summary", "Mình đã hiểu yêu cầu của bạn! Đây là các mentor phù hợp:"),
                    "Mình đã hiểu yêu cầu của bạn! Đây là các mentor phù hợp:"
            );

            if (skills.isEmpty()) {
                return new ChatResponse(
                        "Mình chưa trích xuất được kỹ năng từ hội thoại. Bạn mô tả thêm kỹ năng muốn học nhé!",
                        false,
                        null
                );
            }

            List<MentorMatchDto> mentors = mentorSearchService.findMentors(skills, level);

            if (mentors.isEmpty()) {
                sessionRepository.delete(sessionId);
                return new ChatResponse(
                        "Hiện tại SkillSync chưa có Mentor phù hợp với yêu cầu của bạn. Bạn thử mô tả lại hướng học khác hoặc quay lại sau nhé!",
                        false,
                        null
                );
            }

            List<MentorMatchDto> finalMentors;
            if (configHolder.isEnrichReasonsEnabled()) {
                try {
                    finalMentors = enrichWithReasons(mentors, skills, level, goal);
                } catch (Exception e) {
                    log.warn("Skip reason enrichment (fallback): {}", e.getMessage());
                    finalMentors = attachFallbackReasons(mentors, skills, level, goal);
                }
            } else {
                finalMentors = attachFallbackReasons(mentors, skills, level, goal);
            }

            // Xóa session sau match thành công
            sessionRepository.delete(sessionId);
            return new ChatResponse(summary, true, finalMentors);

        } catch (Exception e) {
            log.error("Error parsing AI search response: {}", e.getMessage(), e);
            return new ChatResponse(
                    "Mình hiểu bạn muốn học IT nhưng gặp lỗi khi tìm kiếm. Thử lại nhé!",
                    false,
                    null
            );
        }
    }

    // ── Internal: Reason enrichment ────────────────────────────────────────

    List<MentorMatchDto> enrichWithReasons(
            List<MentorMatchDto> mentors,
            List<String> skills,
            String level,
            String goal
    ) {
        String enrichPrompt = String.format("""
                        Dựa vào yêu cầu học %s, level %s, mục tiêu "%s",
                        hãy viết 1 câu lý do ngắn gọn cho từng mentor.
                        Tối đa 20 từ mỗi reason.
                        Trả về CHÍNH XÁC JSON array, không thêm markdown:
                        [{"mentorId":"...","reason":"..."}]

                        Danh sách mentor:
                        %s
                        """,
                String.join(", ", skills),
                level,
                goal == null ? "" : goal,
                mentors.stream()
                        .map(m -> String.format(
                                "- ID: %s, Tên: %s, Skill: %s, Level: %s, Kinh nghiệm: %s, Phong cách: %s",
                                m.mentorId(),
                                safe(m.fullName()),
                                safe(m.skillName()),
                                safe(m.level()),
                                safe(m.experienceDesc()),
                                safe(m.teachingStyle())
                        ))
                        .reduce("", (a, b) -> a + "\n" + b)
        );

        try {
            String reasonsJson = groqChatService.generateUserMessage(enrichPrompt);
            String cleaned = reasonsJson.replace("```json", "").replace("```", "").trim();

            List<Map<String, Object>> reasons = objectMapper.readValue(cleaned, new TypeReference<>() {});

            Map<String, String> reasonMap = new HashMap<>();
            for (Map<String, Object> row : reasons) {
                Object mentorId = row.get("mentorId");
                Object reason = row.get("reason");
                if (mentorId != null && reason != null) {
                    reasonMap.put(String.valueOf(mentorId), String.valueOf(reason));
                }
            }

            return mentors.stream()
                    .map(m -> new MentorMatchDto(
                            m.mentorId(),
                            m.fullName(),
                            m.avatarUrl(),
                            m.skillName(),
                            m.level(),
                            m.experienceDesc(),
                            m.teachingStyle(),
                            m.creditsPerHour(),
                            m.rating(),
                            reasonMap.getOrDefault(
                                    m.mentorId().toString(),
                                    buildFallbackReason(m, skills, level, goal)
                            )
                    ))
                    .toList();

        } catch (Exception e) {
            log.warn("Could not enrich mentor reasons: {}", e.getMessage());
            return attachFallbackReasons(mentors, skills, level, goal);
        }
    }

    List<MentorMatchDto> attachFallbackReasons(
            List<MentorMatchDto> mentors,
            List<String> skills,
            String level,
            String goal
    ) {
        return mentors.stream()
                .map(m -> new MentorMatchDto(
                        m.mentorId(),
                        m.fullName(),
                        m.avatarUrl(),
                        m.skillName(),
                        m.level(),
                        m.experienceDesc(),
                        m.teachingStyle(),
                        m.creditsPerHour(),
                        m.rating(),
                        buildFallbackReason(m, skills, level, goal)
                ))
                .toList();
    }

    String buildFallbackReason(MentorMatchDto mentor, List<String> skills, String level, String goal) {
        String skillText = mentor.skillName() != null ? mentor.skillName() : "kỹ năng bạn cần";
        String levelText = level != null ? level : "mục tiêu hiện tại";

        if (goal != null && !goal.isBlank()) {
            return "Phù hợp vì mentor dạy " + skillText + " và có thể hỗ trợ mục tiêu " + goal + ".";
        }
        return "Phù hợp vì mentor dạy " + skillText + " và khá sát với nhu cầu level " + levelText + " của bạn.";
    }

    // ── Internal: JSON parsing ─────────────────────────────────────────────

    String extractSearchJson(String aiResponse) {
        int markerIndex = aiResponse.indexOf("##SEARCH##");
        if (markerIndex < 0) {
            throw new IllegalArgumentException("AI response does not contain ##SEARCH##");
        }

        String tail = aiResponse.substring(markerIndex + "##SEARCH##".length()).trim();
        int start = tail.indexOf('{');
        int end = tail.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("Không tìm thấy JSON hợp lệ sau ##SEARCH##");
        }

        return tail.substring(start, end + 1).trim();
    }

    String safe(String value) {
        return value == null ? "" : value;
    }
}