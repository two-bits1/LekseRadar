package com.lekseradar.service.ai;

/**
 * Ответ модели.
 *
 * @param model      какая модель ответила (сохраняется в chat_messages.model)
 * @param tokensUsed сколько токенов ушло на запрос и ответ, если API это вернул
 */
public record AiReply(String content, String model, Integer tokensUsed) {}
