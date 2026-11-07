package com.lekseradar.dto.ai;

/** Одно сообщение для LLM: role = system | user | assistant. */
public record AiMessageRequestDTO(String role, String content) {
    public static AiMessageRequestDTO system(String c) { return new AiMessageRequestDTO("system", c); }
    public static AiMessageRequestDTO user(String c) { return new AiMessageRequestDTO("user", c); }
    public static AiMessageRequestDTO assistant(String c) { return new AiMessageRequestDTO("assistant", c); }
}
