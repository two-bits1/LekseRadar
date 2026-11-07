package com.lekseradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.dto.ai.AiMessageRequestDTO;
import com.lekseradar.model.ExerciseEntity;
import com.lekseradar.service.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ИИ проверяет ответ на вопрос {@code text} в квизе (выбор вариантов проверяет сама база).
 * Промпт — {@code answer_checker} (ai-content/prompts/answer_checker.md), ответ — {"verdict","explanation"}.
 */
@Service
public class AnswerCheckerService {
    private static final Logger log = LoggerFactory.getLogger(AnswerCheckerService.class);
    private static final Set<String> VERDICTS = Set.of("correct", "partial", "incorrect");
    private static final int ATTEMPTS = 2;

    private final PromptService prompts;
    private final AiService ai;
    private final ObjectMapper mapper;

    public AnswerCheckerService(PromptService prompts, AiService ai, ObjectMapper mapper) {
        this.prompts = prompts;
        this.ai = ai;
        this.mapper = mapper;
    }

    /** @param verdict correct | partial | incorrect */
    public record Check(String verdict, String explanation) {}

    /** @param language язык квиза (quiz_sets.language): на нём ИИ пишет пояснение */
    public Check check(ExerciseEntity question, String answer, String language) {
        String template = prompts.byKey(PromptService.ANSWER_CHECKER);
        String system = PromptService.render(template, Map.of(
                "question", question.getStatement(),
                "correct_answer", question.getCorrectAnswer(),
                "explanation", question.getExplanation() == null ? "" : question.getExplanation(),
                "answer", answer,
                "language", language == null ? "nb" : language));
        List<AiMessageRequestDTO> prompt = List.of(
                AiMessageRequestDTO.system(system),
                AiMessageRequestDTO.user("Проверь ответ ученика. Верни только JSON."));

        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            String content = ai.completeJson(prompt).content();
            Check c = parse(content);
            if (c != null) return c;
            log.warn("answer_checker вернул не тот формат (попытка {}): {}", attempt, content);
        }
        // ИИ дважды ответил не по формату — сравниваем как строки, пояснение возьмёт база из вопроса
        boolean same = norm(answer).equals(norm(question.getCorrectAnswer()));
        return new Check(same ? "correct" : "incorrect", null);
    }

    private Check parse(String content) {
        if (content == null) return null;
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            JsonNode n = mapper.readTree(content.substring(start, end + 1));
            String verdict = n.path("verdict").asText("").strip().toLowerCase(Locale.ROOT);
            if (!VERDICTS.contains(verdict)) return null;
            String explanation = n.path("explanation").asText("").strip();
            return new Check(verdict, explanation.isEmpty() ? null : explanation);
        } catch (Exception e) {
            return null;
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
