package com.lekseradar.ai;

import java.util.List;

/**
 * Заглушка вместо OpenAI: работает без ключа, чтобы фронтенд и API можно было
 * разрабатывать и тестировать локально. Ответы фиксированные.
 */
public class MockAiClient implements AiClient {

    @Override
    public String complete(List<AiMessage> messages) {
        String last = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();
        return """
                **Режим-заглушка** (OPENAI_API_KEY не задан).

                Вы спросили: _%s_

                Так будет выглядеть ответ тьютора:
                1. Разберём условие задачи.
                2. Найдём ключевую идею.
                3. Проверим себя на примере: `2 + 2 = 4`.
                """.formatted(last.length() > 200 ? last.substring(0, 200) + "…" : last);
    }

    @Override
    public String completeJson(List<AiMessage> messages) {
        String system = messages.isEmpty() ? "" : messages.get(0).content();
        if (system.contains(Prompts.CHECK_MARKER)) {
            return """
                    {"correct": true, "score": 80,
                     "feedback": "Заглушка: ответ принят. С ключом OpenAI здесь будет настоящий разбор решения."}
                    """;
        }
        return """
                {"exercises": [
                  {"type": "CHOICE", "question": "Сколько будет 7 × 8?",
                   "options": ["54", "56", "64", "48"], "solution": "56"},
                  {"type": "OPEN", "question": "Объясните своими словами, что такое переменная в программировании.",
                   "options": null, "solution": "Именованная ячейка памяти, в которой хранится значение."}
                ]}
                """;
    }

    @Override
    public String mode() {
        return "mock";
    }
}
