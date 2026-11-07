package com.lekseradar.service.ai;

import com.lekseradar.dto.ai.AiMessageRequestDTO;
import com.lekseradar.dto.ai.AiResponseDTO;
import java.util.List;

/**
 * Доступ к языковой модели. Чтобы подключить другой ИИ (например, Python-сервис),
 * достаточно новой реализации этого интерфейса в {@link com.lekseradar.config.AiConfig}.
 */
public interface AiService {
    /** Обычный текстовый ответ (Markdown). */
    AiResponseDTO complete(List<AiMessageRequestDTO> messages);

    /** Ответ строго в виде JSON-объекта (для генерации квизов и проверки ответов). */
    AiResponseDTO completeJson(List<AiMessageRequestDTO> messages);
}
