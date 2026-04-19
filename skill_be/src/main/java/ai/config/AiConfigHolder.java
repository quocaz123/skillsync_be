package ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * Runtime-mutable holder cho toàn bộ AI config.
 * <p>
 * Được khởi tạo từ application.yaml qua {@code @Value} trong từng service,
 * nhưng admin có thể ghi đè trong runtime thông qua {@link AiConfigController}
 * mà không cần restart server.
 * <p>
 * Thread-safe: các field dùng {@code volatile} vì chỉ có 1 writer (admin) nhiều reader.
 */
@Getter
@Setter
@Component
public class AiConfigHolder {

    // ── Groq ──────────────────────────────────────────────────────────────
    private volatile String groqModel             = "llama-3.3-70b-versatile";
    private volatile double groqTemperature        = 0.2;
    private volatile int    groqMaxOutputTokens    = 768;

    // ── Session memory ────────────────────────────────────────────────────
    private volatile int    sessionTtlMinutes      = 30;
    private volatile int    sessionMaxRecentTurns  = 10;
    private volatile int    sessionSummaryTrigger  = 6;

    // ── Search & Vector ───────────────────────────────────────────────────
    private volatile boolean vectorSearchEnabled        = true;
    private volatile double  vectorSearchThreshold      = 0.80;
    private volatile int     vectorMinPrimaryResults    = 3;

    // ── Feature flags ─────────────────────────────────────────────────────
    private volatile boolean enrichReasonsEnabled  = true;
}
