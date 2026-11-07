package com.lekseradar.ai;

/** Одно сообщение для LLM: role = system | user | assistant. */
public record AiMessage(String role, String content) {
    public static AiMessage system(String c) { return new AiMessage("system", c); }
    public static AiMessage user(String c) { return new AiMessage("user", c); }
    public static AiMessage assistant(String c) { return new AiMessage("assistant", c); }
}
