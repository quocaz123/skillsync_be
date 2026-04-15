package ai.service;

import ai.config.AiConfigHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqChatService {

    private final ObjectMapper objectMapper;
    private final AiConfigHolder configHolder;

    private final RestClient groqRest = RestClient.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .build();

    @Value("${groq.api-key:}")
    private String apiKey;

    public String generateWithHistory(String systemPrompt, List<Turn> history) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình groq.api-key (GROQ_API_KEY).");
        }

        // Đọc từ AiConfigHolder để phản ánh thay đổi runtime
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model",       configHolder.getGroqModel());
        root.put("temperature", configHolder.getGroqTemperature());
        root.put("max_tokens",  configHolder.getGroqMaxOutputTokens());

        ArrayNode messages = objectMapper.createArrayNode();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sys = objectMapper.createObjectNode();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            messages.add(sys);
        }

        for (Turn turn : history) {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", turn.role());
            msg.put("content", turn.text());
            messages.add(msg);
        }

        root.set("messages", messages);

        try {
            String json = groqRest.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(root.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(json);
            JsonNode contentNode = node.path("choices").path(0).path("message").path("content");
            String text = contentNode.asText("");

            if (text.isBlank()) {
                log.warn("Groq returned empty content: {}", json);
            }

            return text;
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi gọi Groq API: " + e.getMessage(), e);
        }
    }

    public String generateUserMessage(String userText) {
        return generateWithHistory("", List.of(Turn.user(userText)));
    }

    public record Turn(String role, String text) {
        public static Turn user(String text)      { return new Turn("user", text); }
        public static Turn assistant(String text)  { return new Turn("assistant", text); }
    }
}
