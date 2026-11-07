package com.lekseradar.dto.ai;

/** Одно сообщение для LLM: role = system | user | assistant. */
public record AiMessageRequest(String role, String content) {
    public static AiMessageRequest system(String c) { return new AiMessageRequest("system", c); }
    public static AiMessageRequest user(String c) { return new AiMessageRequest("user", c); }
    public static AiMessageRequest assistant(String c) { return new AiMessageRequest("assistant", c); }
}
