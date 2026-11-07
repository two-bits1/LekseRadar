package com.lekseradar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lekseradar.model.ChatSessionEntity;
import com.lekseradar.model.enums.ExerciseLevelEnum;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Разбор и исправление JSON квиза от ИИ — без базы и без ИИ. */
class QuizGeneratorServiceTest {
    private final QuizGeneratorService service =
            new QuizGeneratorService(null, null, null, null, null, null, new ObjectMapper());

    @Test
    void parseStripsMarkdownAndText() {
        ObjectNode quiz = service.parse("Вот квиз:\n```json\n{\"title\":\"T\",\"questions\":[]}\n```");
        assertEquals("T", quiz.path("title").asText());
    }

    @Test
    void parseRejectsNonJson() {
        assertThrows(QuizGeneratorService.InvalidQuizException.class, () -> service.parse("не знаю"));
    }

    @Test
    void normalizeKeepsChatSubjectAndFixesAnswers() {
        ChatSessionEntity chat = new ChatSessionEntity(1L, "Quiz", "Brøk");
        chat.setLevel(ExerciseLevelEnum.BEGINNER);
        ObjectNode quiz = service.parse("""
                {"subject":"Математика","topic":"Дроби","level":"???","language":"русский (ru)","title":"Brøker",
                 "questions":[
                   {"type":"single_choice","question":"1/2 = ?","options":["0,5","2"],"correct_answer":["0,5"]},
                   {"type":"text","question":"2/4 = ?","options":["x"],"correct_answer":"1/2"}]}
                """);

        service.normalize(quiz, chat, Map.of("subject", "Matematikk", "language", "русский (ru)",
                "language_code", "ru"), "test-model");

        assertEquals("Matematikk", quiz.path("subject").asText());     // предмет чата, не перевод ИИ
        assertEquals("Brøk", quiz.path("topic").asText());
        assertEquals("beginner", quiz.path("level").asText());          // неверный уровень → уровень чата
        assertEquals("ru", quiz.path("language").asText());              // код из language_code (V20)
        assertEquals("test-model", quiz.path("model").asText());
        assertEquals("0,5", quiz.path("questions").get(0).path("correct_answer").asText());
        assertFalse(quiz.path("questions").get(1).has("options"));
    }

    @Test
    void normalizeFixesAiChoiceMistakes() {
        ObjectNode quiz = service.parse("""
                {"title":"T","questions":[
                  {"type":"single_choice","question":"Велг alle","options":["a","b"," ","c"],"correct_answer":["a","c"]},
                  {"type":"single_choice","question":"Q2","options":["x","","x ","y"],"correct_answer":" y "},
                  {"type":"multiple_choice","question":"Q3","options":["1","2"],"correct_answer":"2"},
                  {"type":"single_choice","question":"Q4","options":["only",""],"correct_answer":"only"},
                  {"type":"single_choice","question":"Q5","options":["a","b"],"correct_answer":"z"}]}
                """);
        service.normalize(quiz, new ChatSessionEntity(1L, "Quiz", "Tema"), Map.of(), "m");
        var qs = quiz.path("questions");
        assertEquals(3, qs.size());                                        // Q4 (1 вариант) и Q5 (нет ответа) убраны
        assertEquals("multiple_choice", qs.get(0).path("type").asText());  // несколько верных → можно выбрать несколько
        assertEquals("[\"a\",\"b\",\"c\"]", qs.get(0).path("options").toString());
        assertEquals("[\"a\",\"c\"]", qs.get(0).path("correct_answer").toString());
        assertEquals("[\"x\",\"y\"]", qs.get(1).path("options").toString()); // пустые и повторы убраны
        assertEquals("y", qs.get(1).path("correct_answer").asText());
        assertEquals("[\"2\"]", qs.get(2).path("correct_answer").toString());
    }

    @Test
    void normalizeRejectsQuizWithoutValidQuestions() {
        ObjectNode quiz = service.parse("""
                {"title":"T","questions":[{"type":"single_choice","question":"Q","options":["a",""],"correct_answer":"a"}]}
                """);
        assertThrows(QuizGeneratorService.InvalidQuizException.class,
                () -> service.normalize(quiz, new ChatSessionEntity(1L, "Quiz", "Tema"), Map.of(), "m"));
    }

    @Test
    void normalizeLimitsQuestionCount() {
        StringBuilder q = new StringBuilder("{\"title\":\"T\",\"questions\":[");
        for (int i = 0; i < 40; i++) {
            if (i > 0) q.append(',');
            q.append("{\"type\":\"text\",\"question\":\"q").append(i).append("\",\"correct_answer\":\"a\"}");
        }
        ObjectNode quiz = service.parse(q.append("]}").toString());
        service.normalize(quiz, new ChatSessionEntity(1L, "Quiz", "Tema"), Map.of(), "m");
        assertEquals(QuizGeneratorService.MAX_COUNT, quiz.path("questions").size());
    }

    @Test
    void renderKeepsUnknownVariables() {
        assertEquals("Hei Ola, {{x}}", PromptService.render("Hei {{name}}, {{x}}", Map.of("name", "Ola")));
    }
}
