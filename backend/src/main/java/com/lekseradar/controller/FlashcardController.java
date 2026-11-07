package com.lekseradar.controller;

import com.lekseradar.dto.dashboard.FlashcardStatsResponseDTO;
import com.lekseradar.dto.flashcard.DeckResponseDTO;
import com.lekseradar.dto.flashcard.FlashcardResponseDTO;
import com.lekseradar.dto.flashcard.ReviewRequestDTO;
import com.lekseradar.dto.flashcard.ReviewResponseDTO;
import com.lekseradar.dto.quiz.StartQuizRequestDTO;
import com.lekseradar.model.UserEntity;
import com.lekseradar.security.CurrentUser;
import com.lekseradar.service.FlashcardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "5. Флеш-карты", description = "Вопросы квизов в режиме повторения (SM-2)")
public class FlashcardController {
    private final FlashcardService flashcards;

    public FlashcardController(FlashcardService flashcards) {
        this.flashcards = flashcards;
    }

    @GetMapping("/flashcards")
    @Operation(summary = "Моя колода", description = "Ближайшие к повторению сверху")
    public List<FlashcardResponseDTO> list(@CurrentUser UserEntity user,
                                           @Parameter(description = "true — только те, что пора повторить")
                                           @RequestParam(defaultValue = "false") boolean due) {
        return flashcards.list(user.getId(), due);
    }

    @GetMapping("/flashcards/due")
    @Operation(summary = "Карточки к повторению сейчас")
    public List<FlashcardResponseDTO> due(@CurrentUser UserEntity user) {
        return flashcards.list(user.getId(), true);
    }

    @GetMapping("/flashcards/stats")
    @Operation(summary = "Сводка по колоде", description = "Всего, к повторению, выучено, повторений")
    public FlashcardStatsResponseDTO stats(@CurrentUser UserEntity user) {
        return flashcards.stats(user.getId());
    }

    @PostMapping("/flashcards/{exerciseId}/review")
    @Operation(summary = "Оценить карточку", description = "0–2 не помню, 3 с трудом, 4 помню, 5 легко")
    public ReviewResponseDTO review(@CurrentUser UserEntity user, @PathVariable Long exerciseId,
                                    @Valid @RequestBody ReviewRequestDTO req) {
        return flashcards.review(user.getId(), exerciseId, req.grade());
    }

    @PostMapping("/flashcards/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Карточки от ИИ по теме",
            description = "Тело как у POST /quizzes. Готовый набор из базы или новый от ИИ → всё в колоду")
    public DeckResponseDTO generate(@CurrentUser UserEntity user, @Valid @RequestBody StartQuizRequestDTO req) {
        return flashcards.generate(user, req);
    }

    @PostMapping("/quiz-sets/{setId}/flashcards")
    @Operation(summary = "Весь квиз в колоду", description = "Кнопка «Учить карточками»")
    public DeckResponseDTO addSet(@CurrentUser UserEntity user, @PathVariable Long setId) {
        return flashcards.addSet(user.getId(), setId);
    }
}
