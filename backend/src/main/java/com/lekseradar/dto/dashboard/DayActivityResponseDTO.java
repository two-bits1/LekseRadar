package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.UserRepository;

import java.time.LocalDate;

/** Активность за день (в часовом поясе пользователя). */
public record DayActivityResponseDTO(LocalDate day, long events, long exercises, long quizzes) {

    public static DayActivityResponseDTO from(UserRepository.DailyActivity d) {
        return new DayActivityResponseDTO(d.getDay(), n(d.getEvents()), n(d.getExercises()), n(d.getQuizzes()));
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
