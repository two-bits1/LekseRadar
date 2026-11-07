package com.lekseradar.progress;

import java.time.LocalDate;
import java.util.List;

public record ProgressDto(
        long exercisesGenerated,
        long attempts,
        long correctAttempts,
        /** Доля верных ответов, 0..1. */
        double accuracy,
        double averageScore,
        long chatSessions,
        long chatQuestions,
        /** Сколько дней подряд (включая сегодня или вчера) были попытки. */
        int streakDays,
        List<SubjectStats> bySubject,
        /** Последние 14 дней, по возрастанию даты. */
        List<DayStats> daily) {

    public record SubjectStats(String subject, long attempts, long correct, double averageScore) {}

    public record DayStats(LocalDate date, long attempts) {}
}
