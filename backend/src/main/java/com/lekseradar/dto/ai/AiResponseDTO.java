package com.lekseradar.dto.ai;

/**
 * Ответ модели.
 *
 * @param model      какая модель ответила (сохраняется в chat_messages.model)
 * @param tokensUsed сколько токенов ушло на запрос и ответ, если API это вернул
 */
public record AiResponseDTO(String content, String model, Integer tokensUsed) {}
