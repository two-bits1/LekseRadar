package com.lekseradar.dto.chat;

/** Ответ на отправку сообщения: обновлённый чат, сохранённый вопрос и ответ ИИ. */
public record ChatReplyResponse(ChatResponse chat, MessageResponse userMessage, MessageResponse assistantMessage) {}
