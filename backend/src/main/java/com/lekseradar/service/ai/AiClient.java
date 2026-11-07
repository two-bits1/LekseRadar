package com.lekseradar.service.ai;

import java.util.List;

/**
 * Доступ к языковой модели. Чтобы подключить другой ИИ (например, Python-сервис),
 * достаточно новой реализации этого интерфейса в {@link com.lekseradar.config.AiConfig}.
 */
public interface AiClient {
    /** Обычный текстовый ответ (Markdown). */
    AiReply complete(List<AiMessage> messages);

    /** Ответ строго в виде JSON-объекта (для генерации квизов и проверки ответов). */
    AiReply completeJson(List<AiMessage> messages);
}
