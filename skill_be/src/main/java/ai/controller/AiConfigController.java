package ai.controller;

import ai.config.AiConfigHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API để admin đọc và cập nhật AI config trong runtime.
 * <p>
 * GET  /api/ai/config        → đọc config hiện tại<br>
 * PUT  /api/ai/config        → ghi đè config (partial update OK)
 */
@RestController
@RequestMapping("/api/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigHolder config;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(toMap());
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body) {
        // ── Groq ──────────────────────────────────────────────────────────
        if (body.containsKey("groqModel"))
            config.setGroqModel((String) body.get("groqModel"));

        if (body.containsKey("groqTemperature"))
            config.setGroqTemperature(toDouble(body.get("groqTemperature")));

        if (body.containsKey("groqMaxOutputTokens"))
            config.setGroqMaxOutputTokens(toInt(body.get("groqMaxOutputTokens")));

        // ── Session ───────────────────────────────────────────────────────
        if (body.containsKey("sessionTtlMinutes"))
            config.setSessionTtlMinutes(toInt(body.get("sessionTtlMinutes")));

        if (body.containsKey("sessionMaxRecentTurns"))
            config.setSessionMaxRecentTurns(toInt(body.get("sessionMaxRecentTurns")));

        if (body.containsKey("sessionSummaryTrigger"))
            config.setSessionSummaryTrigger(toInt(body.get("sessionSummaryTrigger")));

        // ── Search ────────────────────────────────────────────────────────
        if (body.containsKey("vectorSearchEnabled"))
            config.setVectorSearchEnabled((Boolean) body.get("vectorSearchEnabled"));

        if (body.containsKey("vectorSearchThreshold"))
            config.setVectorSearchThreshold(toDouble(body.get("vectorSearchThreshold")));

        if (body.containsKey("vectorMinPrimaryResults"))
            config.setVectorMinPrimaryResults(toInt(body.get("vectorMinPrimaryResults")));

        // ── Feature flags ─────────────────────────────────────────────────
        if (body.containsKey("enrichReasonsEnabled"))
            config.setEnrichReasonsEnabled((Boolean) body.get("enrichReasonsEnabled"));

        return ResponseEntity.ok(toMap());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("groqModel",                config.getGroqModel()),
                Map.entry("groqTemperature",           config.getGroqTemperature()),
                Map.entry("groqMaxOutputTokens",       config.getGroqMaxOutputTokens()),
                Map.entry("sessionTtlMinutes",         config.getSessionTtlMinutes()),
                Map.entry("sessionMaxRecentTurns",     config.getSessionMaxRecentTurns()),
                Map.entry("sessionSummaryTrigger",     config.getSessionSummaryTrigger()),
                Map.entry("vectorSearchEnabled",       config.isVectorSearchEnabled()),
                Map.entry("vectorSearchThreshold",     config.getVectorSearchThreshold()),
                Map.entry("vectorMinPrimaryResults",   config.getVectorMinPrimaryResults()),
                Map.entry("enrichReasonsEnabled",      config.isEnrichReasonsEnabled())
        );
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
    }

    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }
}
