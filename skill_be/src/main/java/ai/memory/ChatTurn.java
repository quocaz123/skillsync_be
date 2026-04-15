package ai.memory;

/**
 * Một lượt hội thoại (user hoặc assistant).
 * Record nhẹ, immutable — dễ serialize vào Redis qua Jackson.
 */
public record ChatTurn(String role, String text) {
    public static ChatTurn user(String text)      { return new ChatTurn("user", text); }
    public static ChatTurn assistant(String text)  { return new ChatTurn("assistant", text); }
}
