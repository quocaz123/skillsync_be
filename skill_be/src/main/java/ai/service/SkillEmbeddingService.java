package ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Gọi Gemini Embedding API để tạo vector embedding cho skill.
 *
 * <p>Thứ tự ưu tiên model (fallback nếu model trước không khả dụng):
 * <ol>
 *   <li>{@code gemini.embedding-model} (mặc định: {@code embedding-001})
 *   <li>{@code text-embedding-004}
 *   <li>{@code embedding-001}
 * </ol>
 *
 * <p>Hiện tại {@code gemini-embedding-001} trả 3072 chiều (theo log runtime),
 * nên DB dùng {@code vector(3072)}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillEmbeddingService {

    /**
     * Danh sách fallback — thứ tự ưu tiên từ trái sang phải.
     * gemini-embedding-001   : stable, thay thế embedding-001 cũ
     * gemini-embedding-2-preview: mới hơn, chiều lớn hơn (1536)
     */
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-embedding-001",
            "gemini-embedding-2-preview"
    );

    private final ObjectMapper objectMapper;

    private final RestClient geminiRest = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    @Value("${gemini.api-key:}")
    private String apiKey;

    /**
     * Model chính được cấu hình; mặc định {@code gemini-embedding-001}.
     */
    @Value("${gemini.embedding-model:gemini-embedding-001}")
    private String embeddingModel;

    /**
     * Số chiều output mong muốn (để phù hợp pgvector index).
     * HNSW của pgvector giới hạn <= 2000 chiều → mặc định 1536.
     */
    @Value("${gemini.embedding-dim:1536}")
    private int embeddingDim;

    /**
     * Cache kết quả probe: null = chưa kiểm tra, true = hoạt động, false = không khả dụng.
     * Tránh gọi API lặp lại khi đã biết không có model nào hoạt động.
     */
    private Boolean probeResult = null;

    /** Model đã được xác nhận hoạt động (set sau khi probe thành công). */
    private String workingModel = null;

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Embed một đoạn text.
     * Gọi {@link #probe()} trước khi loop để tránh 404 lặp lại.
     *
     * @param text chuỗi cần embed
     * @return float[3072] hoặc {@code null} nếu không khả dụng / lỗi
     */
    public float[] embed(String text) {
        if (!isAvailable()) return null;
        if (text == null || text.isBlank()) return null;

        // Dùng model đã biết hoạt động nếu có
        if (workingModel != null) {
            try {
                return parseEmbedding(callEmbedApi(text, workingModel), workingModel);
            } catch (Exception e) {
                log.error("[Embedding] Lỗi model '{}': {}", workingModel, e.getMessage());
                return null;
            }
        }

        // Thử từng model trong fallback list
        for (String model : buildCandidateList()) {
            try {
                float[] vec = parseEmbedding(callEmbedApi(text, model), model);
                if (vec != null) {
                    workingModel = model;
                    return vec;
                }
            } catch (HttpClientErrorException.NotFound ignored) {
            } catch (Exception e) {
                log.error("[Embedding] Lỗi model '{}': {}", model, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Kiểm tra một lần xem API key có hỗ trợ embedding model nào không.
     *
     * <p>Gọi một lần trước khi batch embed để tránh log rác khi không có model nào khả dụng.
     *
     * @return {@code true} nếu có ít nhất 1 model hoạt động
     */
    public boolean probe() {
        if (probeResult != null) return probeResult;
        if (apiKey == null || apiKey.isBlank()) {
            probeResult = false;
            return false;
        }

        log.info("[Embedding] Đang kiểm tra khả năng embedding với API key...");

        for (String model : buildCandidateList()) {
            try {
                log.info("[Embedding] Probe model '{}'...", model);
                String rawJson = callEmbedApi("test", model);
                float[] vec = parseEmbeddingFlexible(rawJson, model);
                if (vec != null) {
                    workingModel = model;
                    probeResult  = true;
                    log.info("[Embedding] ✓ Model '{}' hoạt động ({} chiều).", model, vec.length);
                    return true;
                }
            } catch (HttpClientErrorException.NotFound nf) {
                log.warn("[Embedding] Model '{}' → 404 Not Found (không tồn tại với API key này).", model);
            } catch (HttpClientErrorException e) {
                log.warn("[Embedding] Model '{}' → HTTP {} : {}", model, e.getStatusCode(), e.getMessage());
            } catch (Exception e) {
                log.warn("[Embedding] Model '{}' → Lỗi: {}", model, e.getMessage());
            }
        }

        probeResult = false;
        log.warn("""
                [Embedding] ✗ Không có embedding model nào khả dụng với API key này.
                  → Vector search sẽ bị tắt tự động.
                  → Skills vẫn được seed bình thường (không có embedding).
                  → Để kiểm tra model nào hỗ trợ, gọi:
                    GET https://generativelanguage.googleapis.com/v1beta/models?key=<API_KEY>
                    (tìm entry có supportedGenerationMethods = embedContent)
                """);
        return false;
    }

    /**
     * Kiểm tra nhanh: API key có và probe đã xác nhận hoạt động.
     */
    public boolean isAvailable() {
        if (apiKey == null || apiKey.isBlank()) return false;
        if (probeResult != null) return probeResult;
        return true; // chưa probe → coi như có, embed() sẽ tự xử lý
    }

    /**
     * Xây dựng text mô tả skill để embedding chất lượng cao hơn.
     * Format: "Skill: {name} | Category: {category}"
     */
    public String buildSkillText(String name, String category) {
        return "Skill: " + name + " | Category: " + (category != null ? category : "OTHER");
    }

    // ── Internal ─────────────────────────────────────────────────────────

    /** Model chính đứng đầu, các fallback khác đứng sau (không trùng). */
    private List<String> buildCandidateList() {
        return FALLBACK_MODELS.stream()
                .sorted((a, b) -> a.equals(embeddingModel) ? -1 : b.equals(embeddingModel) ? 1 : 0)
                .distinct()
                .toList();
    }

    private String callEmbedApi(String text, String model) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "models/" + model);

        ObjectNode content = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();
        parts.add(objectMapper.createObjectNode().put("text", text));
        content.set("parts", parts);
        body.set("content", content);

        // Request giảm chiều vector để tương thích pgvector HNSW index
        // (REST field name: outputDimensionality)
        if (embeddingDim > 0) {
            body.put("outputDimensionality", embeddingDim);
        }

        String url = "/v1beta/models/" + model + ":embedContent?key=" + apiKey;

        return geminiRest.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .body(String.class);
    }

    /**
     * Parse embedding với kiểm tra dim == 3072 (dùng khi embed thực tế).
     * DB column là vector(3072) nên từ chối mọi vector có chiều khác.
     */
    private float[] parseEmbedding(String json, String model) throws Exception {
        float[] vec = parseEmbeddingFlexible(json, model);
        if (vec != null && vec.length != embeddingDim) {
            log.warn("[Embedding] Model '{}' trả {} chiều — DB chỉ hỗ trợ {}. Bỏ qua.",
                    model, vec.length, embeddingDim);
            return null;
        }
        return vec;
    }

    /**
     * Parse embedding không giới hạn chiều — dùng trong probe() để phát hiện
     * model hoạt động và log chiều thực tế trả về.
     */
    private float[] parseEmbeddingFlexible(String json, String model) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        if (root.has("error")) {
            log.warn("[Embedding] Model '{}' trả error JSON: {}", model, root.get("error"));
            return null;
        }

        JsonNode values = root.path("embedding").path("values");
        if (!values.isArray() || values.isEmpty()) {
            log.warn("[Embedding] Model '{}' response thiếu embedding.values. Body: {}", model,
                    json.length() > 200 ? json.substring(0, 200) + "..." : json);
            return null;
        }

        int dim = values.size();
        float[] vec = new float[dim];
        for (int i = 0; i < dim; i++) {
            vec[i] = (float) values.get(i).asDouble();
        }

        log.info("[Embedding] Model '{}' → {} chiều.", model, dim);
        return vec;
    }
}
