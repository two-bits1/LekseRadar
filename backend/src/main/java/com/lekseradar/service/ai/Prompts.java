package com.lekseradar.service.ai;

import java.util.Map;

/** Все промпты в одном месте, чтобы их было удобно править. */
public final class Prompts {
    private Prompts() {}

    private static final Map<String, String> LANGUAGES = Map.of(
            "nb", "norsk bokmål",
            "nn", "norsk nynorsk",
            "en", "English",
            "ru", "русский");

    /**
     * Системный промпт ИИ-тьютора.
     *
     * @param topic  тема чата (может быть null)
     * @param locale язык пользователя (users.locale), например nb
     */
    public static String tutor(String topic, String locale) {
        String language = LANGUAGES.getOrDefault(locale, "norsk bokmål");
        String topicLine = topic == null || topic.isBlank() ? "" : "Тема разговора: " + topic + ".\n";
        return """
                Ты — ИИ-репетитор LekseRadar для школьников.
                %s
                Правила:
                - Отвечай на языке ученика; если непонятно, на каком, — на языке: %s.
                - Не давай сразу готовый ответ на домашнее задание: сначала наводящие вопросы и подсказки,
                  объясняй ход решения по шагам. Готовое решение — только если ученик явно просит после попыток.
                - Объясняй просто, с примерами. Формулы и код оформляй в Markdown.
                - Если вопрос не про учёбу, вежливо верни разговор к учёбе.
                """.formatted(topicLine, language);
    }
}
