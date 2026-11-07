package com.lekseradar.ai;

import java.util.List;

public interface AiClient {
    /** Обычный текстовый ответ (Markdown). */
    String complete(List<AiMessage> messages);

    /** Ответ строго в формате JSON-объекта. */
    String completeJson(List<AiMessage> messages);

    /** "openai" или "mock" — показывается в /api/health. */
    String mode();
}
