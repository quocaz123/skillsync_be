package ai.memory;

import ai.service.GroqChatService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Xây dựng danh sách {@link GroqChatService.Turn} từ {@link AiSessionState}.
 * <p>
 * Thứ tự inject vào prompt:
 * <ol>
 *   <li>System context (summary cũ, nếu có) — dưới dạng user turn mang context</li>
 *   <li>Recent turns (history)</li>
 *   <li>User message hiện tại</li>
 * </ol>
 */
@Component
public class MemoryPromptBuilder {

    /**
     * Tạo danh sách turn để truyền vào {@code GroqChatService.generateWithHistory()}.
     *
     * @param state       trạng thái session hiện tại
     * @param userMessage tin nhắn hiện tại của user
     * @return list turn đầy đủ, sẵn sàng gọi LLM
     */
    public List<GroqChatService.Turn> buildTurns(AiSessionState state, String userMessage) {
        List<GroqChatService.Turn> turns = new ArrayList<>();

        // 1️⃣ Inject summary cũ như một user-turn đặc biệt (context cho LLM)
        if (state.getSummary() != null && !state.getSummary().isBlank()) {
            turns.add(GroqChatService.Turn.user(
                    "[Tóm tắt hội thoại trước: " + state.getSummary() + "]"
            ));
            // Fake assistant ack để LLM không bị confused
            turns.add(GroqChatService.Turn.assistant("Đã ghi nhận bối cảnh trước đó."));
        }

        // 2️⃣ Inject recent turns (history đã được trim)
        if (state.getRecentTurns() != null) {
            for (ChatTurn ct : state.getRecentTurns()) {
                if ("user".equals(ct.role())) {
                    turns.add(GroqChatService.Turn.user(ct.text()));
                } else {
                    turns.add(GroqChatService.Turn.assistant(ct.text()));
                }
            }
        }

        // 3️⃣ Thêm user message hiện tại
        turns.add(GroqChatService.Turn.user(userMessage));

        return turns;
    }
}
