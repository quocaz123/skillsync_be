package ai.memory;

import ai.service.GroqChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tóm tắt hội thoại dài thành đoạn ngắn để giảm token khi gọi LLM.
 * <p>
 * Được trigger bởi {@code ConversationalRagService} sau mỗi N lượt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryService {

    private final GroqChatService groqChatService;

    /**
     * Tóm tắt hội thoại.
     *
     * @param previousSummary tóm tắt cũ (có thể null/blank)
     * @param turns           các lượt cần tóm tắt
     * @return chuỗi tóm tắt mới (< 80 từ)
     */
    public String refreshSummary(String previousSummary, List<ChatTurn> turns) {
        String historyText = turns.stream()
                .map(t -> (t.role().equals("user") ? "User: " : "AI: ") + t.text())
                .collect(Collectors.joining("\n"));

        String contextLine = (previousSummary != null && !previousSummary.isBlank())
                ? "Tóm tắt trước đó: " + previousSummary + "\n\n"
                : "";

        String prompt = String.format("""
                %sDưới đây là phần hội thoại mới nhất:
                %s
                
                Hãy tóm tắt ngắn gọn (tối đa 80 từ, tiếng Việt) nội dung người dùng cần học, \
                kỹ năng quan tâm, và bất kỳ ràng buộc quan trọng nào đã đề cập. \
                Không thêm lời chào hay giải thích thêm. Chỉ trả về đoạn tóm tắt.
                """, contextLine, historyText);

        try {
            String summary = groqChatService.generateUserMessage(prompt);
            log.debug("Session summary refreshed: {} chars", summary.length());
            return summary.trim();
        } catch (Exception e) {
            log.warn("SessionSummaryService: không tóm tắt được, giữ summary cũ. Lỗi: {}", e.getMessage());
            return previousSummary != null ? previousSummary : "";
        }
    }
}
