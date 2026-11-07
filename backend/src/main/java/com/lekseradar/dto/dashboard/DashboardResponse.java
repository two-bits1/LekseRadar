package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

/** Сводка для главной страницы и экрана прогресса ({@code GET /dashboard}). */
public record DashboardResponse(long homeworkTodo, long homeworkOverdue, long homeworkDone,
                                long quizzesCompleted, BigDecimal avgQuizScore, long exercisesAnswered,
                                long questionsAsked, long activeDays30, FlashcardStatsResponse flashcards,
                                List<TopicProgressResponse> topics, List<DayActivityResponse> days) {

    /** @param d строка вида user_dashboard или null, если у пользователя ещё нет данных */
    public static DashboardResponse from(UserRepository.Dashboard d, FlashcardStatsResponse flashcards,
                                         List<TopicProgressResponse> topics, List<DayActivityResponse> days) {
        if (d == null) {
            return new DashboardResponse(0, 0, 0, 0, null, 0, 0, 0, flashcards, topics, days);
        }
        return new DashboardResponse(n(d.getHomeworkTodo()), n(d.getHomeworkOverdue()), n(d.getHomeworkDone()),
                n(d.getQuizzesCompleted()), d.getAvgQuizScore(), n(d.getExercisesAnswered()),
                n(d.getQuestionsAsked()), n(d.getActiveDays30()), flashcards, topics, days);
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
