package com.lekseradar.controller;

import com.lekseradar.dto.quiz.*;
import com.lekseradar.model.UserEntity;
import com.lekseradar.security.CurrentUser;
import com.lekseradar.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "4. Квизы", description = "ИИ составляет вопросы по промпту quiz_generator, проверяет текстовые ответы")
public class QuizController {
    private final QuizService quizzes;

    public QuizController(QuizService quizzes) {
        this.quizzes = quizzes;
    }

    @GetMapping("/subjects")
    @Operation(summary = "Предметы", description = "Все предметы из базы, по алфавиту")
    public List<SubjectResponseDTO> subjects() {
        return quizzes.subjects();
    }

    @GetMapping("/quizzes")
    @Operation(summary = "Мои квизы", description = "Новые сверху, без вопросов")
    public List<QuizSummaryResponseDTO> list(@CurrentUser UserEntity user) {
        return quizzes.list(user.getId());
    }

    @GetMapping("/quizzes/progress")
    @Operation(summary = "Прогресс по предметам и темам", description = "Лучший и последний балл, сколько квизов")
    public List<QuizProgressResponseDTO> progress(@CurrentUser UserEntity user) {
        return quizzes.progress(user.getId());
    }

    @PostMapping("/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Начать квиз",
            description = "Готовый набор из базы или новый от ИИ (5–30 секунд). Незаконченный квиз продолжается.")
    public QuizResponseDTO start(@CurrentUser UserEntity user, @Valid @RequestBody StartQuizRequestDTO req) {
        return quizzes.start(user, req);
    }

    @PostMapping("/quiz-sets/{setId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Начать квиз по набору", description = "Например, chat.quizSetId из квиз-чата")
    public QuizResponseDTO startFromSet(@CurrentUser UserEntity user, @PathVariable Long setId) {
        return quizzes.startFromSet(user.getId(), setId);
    }

    @GetMapping("/quizzes/{quizId}")
    @Operation(summary = "Квиз с вопросами", description = "Без правильных ответов")
    public QuizResponseDTO get(@CurrentUser UserEntity user, @PathVariable Long quizId) {
        return quizzes.get(user.getId(), quizId);
    }

    @PostMapping("/quizzes/{quizId}/answers")
    @Operation(summary = "Ответить на вопрос",
            description = "Выбор проверяет база, текст — ИИ (answer_checker). multiple_choice: [\"A\",\"C\"]")
    public AnswerResponseDTO answer(@CurrentUser UserEntity user, @PathVariable Long quizId,
                                    @Valid @RequestBody AnswerRequestDTO req) {
        return quizzes.answer(user.getId(), quizId, req);
    }

    @PostMapping("/quizzes/{quizId}/finish")
    @Operation(summary = "Завершить квиз", description = "Балл 0–100, partial = 0,5")
    public FinishResponseDTO finish(@CurrentUser UserEntity user, @PathVariable Long quizId) {
        return quizzes.finish(user.getId(), quizId);
    }
}
