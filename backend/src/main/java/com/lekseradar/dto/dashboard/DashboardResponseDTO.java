package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

/** Сводка для главной страницы и экрана прогресса ({@code GET /dashboard}). */
public record DashboardResponseDTO(long homeworkTodo, long homeworkOverdue, long homeworkDone,
                                long quizzesCompleted, BigDecimal avgQuizScore, long exercisesAnswered,
                                long questionsAsked, long activeDays30, FlashcardStatsResponseDTO flashcards,
                                List<TopicProgressResponseDTO> topics, List<DayActivityResponseDTO> days) {

    /** @param d строка вида user_dashboard или null, если у пользователя ещё нет данных */
    public static DashboardResponseDTO from(UserRepository.Dashboard d, FlashcardStatsResponseDTO flashcards,
                                         List<TopicProgressResponseDTO> topics, List<DayActivityResponseDTO> days) {
        if (d == null) {
            return new DashboardResponseDTO(0, 0, 0, 0, null, 0, 0, 0, flashcards, topics, days);
        }
        return new DashboardResponseDTO(n(d.getHomeworkTodo()), n(d.getHomeworkOverdue()), n(d.getHomeworkDone()),
                n(d.getQuizzesCompleted()), d.getAvgQuizScore(), n(d.getExercisesAnswered()),
                n(d.getQuestionsAsked()), n(d.getActiveDays30()), flashcards, topics, days);
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
