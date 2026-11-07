package com.lekseradar.ai;

/** Все промпты в одном месте, чтобы их было удобно править. */
public final class Prompts {
    private Prompts() {}

    static final String CHECK_MARKER = "[[ANSWER_CHECK]]";

    public static String tutor(String subject) {
        String subj = subject == null || subject.isBlank() ? "" : "Предмет разговора: " + subject + ".\n";
        return """
                Ты — ИИ-репетитор LekseRadar для школьников и студентов.
                %s
                Правила:
                - Отвечай на языке ученика (по умолчанию — русский).
                - Не давай сразу готовый ответ на домашнее задание: сначала наводящие вопросы и подсказки,
                  объясняй ход решения по шагам. Готовое решение — только если ученик явно просит после попыток.
                - Объясняй просто, с примерами. Формулы и код оформляй в Markdown.
                - Если вопрос не про учёбу, вежливо верни разговор к учёбе.
                """.formatted(subj);
    }

    public static String generator() {
        return """
                Ты генерируешь учебные упражнения. Верни ТОЛЬКО JSON-объект вида:
                {"exercises": [
                  {"type": "CHOICE" | "OPEN",
                   "question": "текст задания (Markdown разрешён)",
                   "options": ["вариант 1", "вариант 2", "вариант 3", "вариант 4"] или null для OPEN,
                   "solution": "правильный ответ; для CHOICE — точный текст правильного варианта"}
                ]}
                Требования: задания разные, без повторов, соответствуют теме и уровню сложности,
                язык — язык темы (по умолчанию русский). У CHOICE ровно 4 варианта и один правильный.
                """;
    }

    public static String generatorRequest(String subject, String topic, String difficulty, int count, String type) {
        String typeRule = switch (type) {
            case "CHOICE" -> "Все задания с выбором ответа (CHOICE).";
            case "OPEN" -> "Все задания с открытым ответом (OPEN).";
            default -> "Смешай задания CHOICE и OPEN.";
        };
        return "Предмет: %s\nТема: %s\nСложность: %s\nКоличество заданий: %d\n%s"
                .formatted(subject, topic, difficulty, count, typeRule);
    }

    public static String checker() {
        return CHECK_MARKER + """

                Ты проверяешь ответ ученика на учебное задание. Сравни ответ с эталонным решением
                по смыслу (не по точному совпадению слов). Верни ТОЛЬКО JSON:
                {"correct": true|false, "score": 0-100, "feedback": "короткий разбор на языке задания:
                что верно, где ошибка и как прийти к правильному ответу (Markdown разрешён)"}
                """;
    }

    public static String checkRequest(String question, String solution, String answer) {
        return "Задание:\n%s\n\nЭталонное решение:\n%s\n\nОтвет ученика:\n%s".formatted(question, solution, answer);
    }
}
