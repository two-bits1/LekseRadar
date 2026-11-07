package com.lekseradar.exercise;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.ai.AiClient;
import com.lekseradar.ai.AiMessage;
import com.lekseradar.ai.Prompts;
import com.lekseradar.common.ApiException;
import com.lekseradar.exercise.ExerciseDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExerciseService {
    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final ExerciseRepository exercises;
    private final ExerciseAttemptRepository attempts;
    private final AiClient ai;
    private final ObjectMapper mapper;

    public ExerciseService(ExerciseRepository exercises, ExerciseAttemptRepository attempts, AiClient ai,
                           ObjectMapper mapper) {
        this.exercises = exercises;
        this.attempts = attempts;
        this.ai = ai;
        this.mapper = mapper;
    }

    public List<ExerciseDto> generate(Long userId, GenerateRequest req) {
        String type = req.type() == null ? "MIXED" : req.type().toUpperCase(Locale.ROOT);
        String raw = ai.completeJson(List.of(
                AiMessage.system(Prompts.generator()),
                AiMessage.user(Prompts.generatorRequest(req.subject(), req.topic(), req.difficulty().name(),
                        req.count(), type))));

        List<Exercise> created = new ArrayList<>();
        for (JsonNode n : parse(raw).path("exercises")) {
            String question = n.path("question").asText("").strip();
            String solution = n.path("solution").asText("").strip();
            if (question.isEmpty() || solution.isEmpty()) continue;

            List<String> options = null;
            if (n.path("options").isArray() && !n.path("options").isEmpty()) {
                options = new ArrayList<>();
                for (JsonNode o : n.path("options")) options.add(o.asText());
            }
            Exercise.Type t = options != null && "CHOICE".equalsIgnoreCase(n.path("type").asText())
                    ? Exercise.Type.CHOICE : Exercise.Type.OPEN;
            if (t == Exercise.Type.OPEN) options = null;

            created.add(new Exercise(userId, req.subject().trim(), req.topic().trim(), req.difficulty(), t,
                    question, options, solution));
            if (created.size() >= req.count()) break;
        }
        if (created.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ИИ не смог сгенерировать задания, попробуйте ещё раз");
        }
        return exercises.saveAll(created).stream().map(e -> ExerciseDto.from(e, List.of())).toList();
    }

    public PageDto<ExerciseDto> list(Long userId, String subject, int page, int size) {
        Page<Exercise> p = exercises.findForUser(userId, blankToNull(subject),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        List<Long> ids = p.getContent().stream().map(Exercise::getId).toList();
        Map<Long, List<AttemptDto>> byExercise = ids.isEmpty() ? Map.of()
                : attempts.findByExerciseIdInOrderByCreatedAtDesc(ids).stream()
                .collect(Collectors.groupingBy(ExerciseAttempt::getExerciseId, LinkedHashMap::new,
                        Collectors.mapping(AttemptDto::from, Collectors.toList())));
        List<ExerciseDto> items = p.getContent().stream()
                .map(e -> ExerciseDto.from(e, byExercise.getOrDefault(e.getId(), List.of()))).toList();
        return new PageDto<>(items, p.getNumber(), p.getSize(), p.getTotalElements());
    }

    public ExerciseDto get(Long userId, Long id) {
        Exercise e = owned(userId, id);
        return ExerciseDto.from(e, attemptsOf(id));
    }

    public List<String> subjects(Long userId) {
        return exercises.findSubjects(userId);
    }

    public ExerciseDto answer(Long userId, Long id, String answer) {
        Exercise e = owned(userId, id);

        boolean correct;
        int score;
        String feedback;
        try {
            JsonNode res = parse(ai.completeJson(List.of(
                    AiMessage.system(Prompts.checker()),
                    AiMessage.user(Prompts.checkRequest(questionWithOptions(e), e.getSolution(), answer)))));
            correct = res.path("correct").asBoolean(false);
            score = Math.max(0, Math.min(100, res.path("score").asInt(correct ? 100 : 0)));
            feedback = res.path("feedback").asText("").strip();
        } catch (ApiException ex) {
            if (e.getType() != Exercise.Type.CHOICE) throw ex;
            // Для выбора ответа можно проверить и без ИИ
            correct = false;
            score = 0;
            feedback = "";
        }

        if (e.getType() == Exercise.Type.CHOICE) {
            // Для вариантов ответа правильность определяем сами, ИИ даёт только объяснение
            correct = normalize(answer).equals(normalize(e.getSolution()));
            score = correct ? 100 : 0;
            if (feedback.isEmpty()) {
                feedback = correct ? "Верно!" : "Неверно. Правильный ответ: " + e.getSolution();
            }
        }
        if (feedback.isEmpty()) feedback = correct ? "Верно!" : "Есть ошибки, сравните с эталонным решением.";

        attempts.save(new ExerciseAttempt(e.getId(), userId, answer, correct, score, feedback));
        return ExerciseDto.from(e, attemptsOf(id));
    }

    private List<AttemptDto> attemptsOf(Long exerciseId) {
        return attempts.findByExerciseIdOrderByCreatedAtDesc(exerciseId).stream().map(AttemptDto::from).toList();
    }

    private Exercise owned(Long userId, Long id) {
        return exercises.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Упражнение"));
    }

    private JsonNode parse(String raw) {
        try {
            String s = raw.strip();
            // На случай, если модель обернула JSON в ```json ... ```
            int start = s.indexOf('{');
            int end = s.lastIndexOf('}');
            if (start >= 0 && end > start) s = s.substring(start, end + 1);
            return mapper.readTree(s);
        } catch (Exception ex) {
            log.warn("Cannot parse AI JSON: {}", raw);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ИИ вернул ответ в неожиданном формате, попробуйте ещё раз");
        }
    }

    private static String questionWithOptions(Exercise e) {
        if (e.getOptions() == null) return e.getQuestion();
        return e.getQuestion() + "\nВарианты: " + String.join(" | ", e.getOptions());
    }

    private static String normalize(String s) {
        return s == null ? "" : s.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
