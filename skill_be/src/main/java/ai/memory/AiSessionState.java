package ai.memory;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Trạng thái phiên hội thoại AI — lưu vào Redis.
 * <p>
 * Implements {@link Serializable} để tương thích với cả Java serialization
 * lẫn Jackson (Jackson không bắt buộc, nhưng không gây conflict).
 * Khi dùng {@code Jackson2JsonRedisSerializer} thì Jackson làm việc chính.
 */
@Data
public class AiSessionState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tóm tắt các phần hội thoại cũ (do SessionSummaryService tạo ra). */
    private String summary;

    /** N lượt hội thoại gần nhất (user + assistant xen kẽ). */
    private List<ChatTurn> recentTurns = new ArrayList<>();

    /** Timestamp lần cuối gửi request — dùng để rate-limit guard. */
    private long lastRequestAt = 0L;

    /** Timestamp lần cuối truy cập — dùng để tính TTL khi cần. */
    private long lastAccessAt = System.currentTimeMillis();
}
