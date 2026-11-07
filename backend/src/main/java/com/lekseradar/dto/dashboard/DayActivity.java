package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.UserRepository;

import java.time.LocalDate;

/** Активность за день (в часовом поясе пользователя). */
public record DayActivity(LocalDate day, long events, long exercises, long quizzes) {

    public static DayActivity from(UserRepository.DailyActivity d) {
        return new DayActivity(d.getDay(), n(d.getEvents()), n(d.getExercises()), n(d.getQuizzes()));
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
