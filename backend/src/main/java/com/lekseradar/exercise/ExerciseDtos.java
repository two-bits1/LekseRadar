package com.lekseradar.exercise;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class ExerciseDtos {
    private ExerciseDtos() {}

    public record GenerateRequest(
            @NotBlank @Size(max = 100) String subject,
            @NotBlank @Size(max = 200) String topic,
            @NotNull Exercise.Difficulty difficulty,
            @Min(1) @Max(10) int count,
            /** CHOICE, OPEN или MIXED (по умолчанию). */
            String type) {}

    public record AnswerRequest(@NotBlank @Size(max = 8000) String answer) {}

    public record AttemptDto(Long id, String answer, boolean correct, int score, String feedback, Instant createdAt) {
        static AttemptDto from(ExerciseAttempt a) {
            return new AttemptDto(a.getId(), a.getAnswer(), a.isCorrect(), a.getScore(), a.getFeedback(),
                    a.getCreatedAt());
        }
    }

    /**
     * Эталонное решение отдаётся только после первой попытки, чтобы его нельзя было подсмотреть.
     */
    public record ExerciseDto(Long id, String subject, String topic, String difficulty, String type,
                              String question, List<String> options, String solution,
                              Instant createdAt, List<AttemptDto> attempts) {
        static ExerciseDto from(Exercise e, List<AttemptDto> attempts) {
            return new ExerciseDto(e.getId(), e.getSubject(), e.getTopic(), e.getDifficulty().name(),
                    e.getType().name(), e.getQuestion(), e.getOptions(),
                    attempts.isEmpty() ? null : e.getSolution(), e.getCreatedAt(), attempts);
        }
    }

    public record PageDto<T>(List<T> items, int page, int size, long total) {}
}
