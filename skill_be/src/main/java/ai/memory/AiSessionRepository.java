package ai.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis access layer cho AI session state.
 * <p>
 * Thay thế hoàn toàn {@code ConcurrentHashMap<String, SessionState>}
 * cũ trong {@code ConversationalRagService}.
 * <p>
 * Key pattern: {@code ai:session:<sessionId>}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiSessionRepository {

    private static final String KEY_PREFIX = "ai:session:";

    @Value("${skillsync.ai.session.ttl-minutes:30}")
    private int ttlMinutes;

    @Value("${skillsync.ai.session.max-recent-turns:10}")
    private int maxRecentTurns;

    private final RedisTemplate<String, AiSessionState> aiSessionRedisTemplate;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Lấy session từ Redis; nếu không tồn tại trả về state mới (chưa lưu).
     */
    public AiSessionState getOrCreate(String sessionId) {
        try {
            AiSessionState state = aiSessionRedisTemplate.opsForValue().get(key(sessionId));
            if (state != null) {
                log.debug("Redis HIT session={}", sessionId);
                return state;
            }
        } catch (Exception e) {
            log.warn("Redis GET failed for session={}, fallback to new state: {}", sessionId, e.getMessage());
        }
        log.debug("Redis MISS session={}, creating new AiSessionState", sessionId);
        return new AiSessionState();
    }

    /**
     * Lưu session vào Redis với TTL.
     * Tự động trim {@code recentTurns} về {@code maxRecentTurns} trước khi lưu.
     */
    public void save(String sessionId, AiSessionState state) {
        // Trim recentTurns để tránh bloat
        List<ChatTurn> turns = state.getRecentTurns();
        if (turns != null && turns.size() > maxRecentTurns) {
            state.setRecentTurns(new ArrayList<>(turns.subList(turns.size() - maxRecentTurns, turns.size())));
        }
        state.setLastAccessAt(System.currentTimeMillis());

        try {
            aiSessionRedisTemplate.opsForValue().set(key(sessionId), state, Duration.ofMinutes(ttlMinutes));
            log.debug("Redis SET session={}, turns={}, ttl={}m", sessionId,
                    state.getRecentTurns().size(), ttlMinutes);
        } catch (Exception e) {
            log.error("Redis SET failed for session={}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Xóa session khỏi Redis (sau khi tìm mentor thành công hoặc reset).
     */
    public void delete(String sessionId) {
        try {
            Boolean deleted = aiSessionRedisTemplate.delete(key(sessionId));
            log.debug("Redis DEL session={}, success={}", sessionId, deleted);
        } catch (Exception e) {
            log.warn("Redis DEL failed for session={}: {}", sessionId, e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
