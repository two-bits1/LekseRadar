package com.lekseradar.service.ai;

import com.lekseradar.dto.ai.AiMessageRequest;
import com.lekseradar.dto.ai.AiResponse;
import java.util.List;

/**
 * Доступ к языковой модели. Чтобы подключить другой ИИ (например, Python-сервис),
 * достаточно новой реализации этого интерфейса в {@link com.lekseradar.config.AiConfig}.
 */
public interface AiService {
    /** Обычный текстовый ответ (Markdown). */
    AiResponse complete(List<AiMessageRequest> messages);

    /** Ответ строго в виде JSON-объекта (для генерации квизов и проверки ответов). */
    AiResponse completeJson(List<AiMessageRequest> messages);
}
