package com.lekseradar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.dto.quiz.*;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.model.ExerciseEntity;
import com.lekseradar.model.QuizEntity;
import com.lekseradar.model.QuizQuestionEntity;
import com.lekseradar.model.QuizSetEntity;
import com.lekseradar.model.SubjectEntity;
import com.lekseradar.model.UserEntity;
import com.lekseradar.model.enums.QuestionTypeEnum;
import com.lekseradar.model.enums.QuizStatusEnum;
import com.lekseradar.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Квизы: ИИ составляет вопросы ({@link QuizGeneratorService}), база проверяет выбор вариантов,
 * ИИ проверяет текстовые ответы ({@link AnswerCheckerService}). Неверные ответы база сама превращает в карточки.
 */
@Service
public class QuizService {
    private final QuizRepository quizzes;
    private final QuizSetRepository sets;
    private final QuizQuestionRepository quizQuestions;
    private final ExerciseRepository exercises;
    private final SubjectRepository subjects;
    private final ChatSessionRepository chats;
    private final QuizGeneratorService generator;
    private final AnswerCheckerService checker;
    private final ObjectMapper mapper;

    public QuizService(QuizRepository quizzes, QuizSetRepository sets, QuizQuestionRepository quizQuestions,
                       ExerciseRepository exercises, SubjectRepository subjects, ChatSessionRepository chats,
                       QuizGeneratorService generator, AnswerCheckerService checker, ObjectMapper mapper) {
        this.quizzes = quizzes;
        this.sets = sets;
        this.quizQuestions = quizQuestions;
        this.exercises = exercises;
        this.subjects = subjects;
        this.chats = chats;
        this.generator = generator;
        this.checker = checker;
        this.mapper = mapper;
    }

    public List<SubjectResponseDTO> subjects() {
        return subjects.findAllByOrderByName().stream().map(SubjectResponseDTO::from).toList();
    }

    public List<QuizSummaryResponseDTO> list(Long userId) {
        List<QuizEntity> found = quizzes.findByUserIdOrderByStartedAtDesc(userId);
        Map<Long, String> names = subjectNames();
        Map<Long, Integer> counts = sets.findAllById(found.stream().map(QuizEntity::getQuizSetId)
                        .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(QuizSetEntity::getId, QuizSetEntity::getQuestionCount));
        return found.stream().map(q -> QuizSummaryResponseDTO.from(q, names.get(q.getSubjectId()),
                counts.getOrDefault(q.getQuizSetId(), 0))).toList();
    }

    public List<QuizProgressResponseDTO> progress(Long userId) {
        return quizzes.findProgress(userId).stream().map(QuizProgressResponseDTO::from).toList();
    }

    /** Готовый набор из базы или новый от ИИ → начать (или продолжить незаконченный). */
    public QuizResponseDTO start(UserEntity user, StartQuizRequestDTO req) {
        QuizGeneratorService.SetResult set = generator.findOrGenerate(user, req);
        Long quizId = quizzes.start(user.getId(), set.quizSetId());
        quizzes.link(quizId, set.chatId(), req.assignmentId());
        return get(user.getId(), quizId);
    }

    /** Начать квиз по готовому набору (например, {@code chat.quizSetId} из квиз-чата). */
    public QuizResponseDTO startFromSet(Long userId, Long setId) {
        if (!sets.existsById(setId)) throw new NotFoundException("Набор вопросов не найден");
        Long quizId = quizzes.start(userId, setId);
        chats.findFirstByUserIdAndQuizSetIdOrderByUpdatedAtDesc(userId, setId)
                .ifPresent(c -> quizzes.link(quizId, c.getId(), c.getAssignmentId()));
        return get(userId, quizId);
    }

    public QuizResponseDTO get(Long userId, Long quizId) {
        QuizEntity q = owned(userId, quizId);
        List<QuestionResponseDTO> questions = quizzes.findPublicQuestions(quizId).stream()
                .map(p -> QuestionResponseDTO.from(p, options(p.getOptions()))).toList();
        return QuizResponseDTO.from(q, subjectName(q.getSubjectId()), questions);
    }

    /**
     * Ответ на вопрос: single/multiple_choice проверяет база, text — ИИ по промпту answer_checker.
     * В ответе вердикт, пояснение и правильный ответ.
     */
    public AnswerResponseDTO answer(Long userId, Long quizId, AnswerRequestDTO req) {
        QuizEntity q = owned(userId, quizId);
        if (q.getStatus() != QuizStatusEnum.IN_PROGRESS) {
            throw new BadRequestException("Квиз уже завершён");
        }
        QuizQuestionEntity link = quizQuestions.findByQuizIdOrderByPosition(quizId).stream()
                .filter(x -> x.getPosition() == req.position()).findFirst()
                .orElseThrow(() -> new BadRequestException("В квизе нет вопроса " + req.position()));
        ExerciseEntity e = exercises.findById(link.getExerciseId())
                .orElseThrow(() -> new NotFoundException("Вопрос не найден"));

        String answer = req.answer().strip();
        String verdict = null;
        String explanation = null;
        if (e.getQuestionType() == QuestionTypeEnum.MULTIPLE_CHOICE) {
            answer = jsonArray(answer);
        } else if (e.getQuestionType() == QuestionTypeEnum.TEXT) {
            AnswerCheckerService.Check c = checker.check(e, answer, language(q));
            verdict = c.verdict();
            explanation = c.explanation();
        }
        return AnswerResponseDTO.from(quizzes.saveAnswer(quizId, req.position(), answer, verdict, explanation));
    }

    /** Итоговый балл 0–100. Повторный вызов вернёт тот же балл. */
    public FinishResponseDTO finish(Long userId, Long quizId) {
        QuizEntity q = owned(userId, quizId);
        if (q.getStatus() == QuizStatusEnum.COMPLETED) {
            return new FinishResponseDTO(q.getScore());
        }
        BigDecimal score = quizzes.finish(quizId);
        return new FinishResponseDTO(score);
    }

    private QuizEntity owned(Long userId, Long quizId) {
        return quizzes.findByIdAndUserId(quizId, userId).orElseThrow(() -> new NotFoundException("Квиз не найден"));
    }

    private String language(QuizEntity q) {
        return q.getQuizSetId() == null ? null
                : sets.findById(q.getQuizSetId()).map(QuizSetEntity::getLanguage).orElse(null);
    }

    /** multiple_choice: ответ — JSON-массив строк, например ["A","C"]. */
    private String jsonArray(String answer) {
        try {
            JsonNode n = mapper.readTree(answer);
            if (n != null && n.isArray() && n.size() > 0) {
                for (JsonNode x : n) if (!x.isTextual()) throw new IllegalArgumentException();
                return mapper.writeValueAsString(n);
            }
        } catch (Exception ignored) {
            // ниже — понятная ошибка
        }
        throw new BadRequestException("Для вопроса с несколькими ответами нужен JSON-массив: [\"вариант 1\",\"вариант 2\"]");
    }

    private List<String> options(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private Map<Long, String> subjectNames() {
        return subjects.findAll().stream().collect(Collectors.toMap(SubjectEntity::getId, SubjectEntity::getName));
    }

    private String subjectName(Long id) {
        return id == null ? null : subjects.findById(id).map(SubjectEntity::getName).orElse(null);
    }
}
