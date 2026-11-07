package com.lekseradar.dto.chat;

/** Ответ на отправку сообщения: обновлённый чат, сохранённый вопрос и ответ ИИ. */
public record ChatReplyResponseDTO(ChatResponseDTO chat, MessageResponseDTO userMessage, MessageResponseDTO assistantMessage) {}
