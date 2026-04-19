package ai.controller;

import ai.dto.request.ChatRequest;
import ai.dto.response.ChatResponse;
import ai.service.ConversationalRagService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MentorAiChatController {
    ConversationalRagService conversationalRagService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || request.sessionId() == null || request.sessionId().isBlank()) {
            return ResponseEntity.badRequest().body(new ChatResponse(
                    "Thiếu `sessionId`. Bạn hãy gửi body dạng: {\"sessionId\":\"<uuid>\",\"message\":\"...\"}",
                    false,
                    null
            ));
        }
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(new ChatResponse(
                    "Thiếu `message`.",
                    false,
                    null
            ));
        }
        ChatResponse response = conversationalRagService.processMessage(
                request.sessionId(),
                request.message()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/chat/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        conversationalRagService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
