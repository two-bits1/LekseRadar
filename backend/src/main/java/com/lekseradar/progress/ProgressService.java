package com.lekseradar.progress;

import com.lekseradar.chat.ChatMessageRepository;
import com.lekseradar.chat.ChatSessionRepository;
import com.lekseradar.exercise.ExerciseAttemptRepository;
import com.lekseradar.exercise.ExerciseRepository;
import com.lekseradar.progress.ProgressDto.DayStats;
import com.lekseradar.progress.ProgressDto.SubjectStats;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class ProgressService {
    private static final int DAYS = 14;

    private final ExerciseRepository exercises;
    private final ExerciseAttemptRepository attempts;
    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;

    public ProgressService(ExerciseRepository exercises, ExerciseAttemptRepository attempts,
                           ChatSessionRepository sessions, ChatMessageRepository messages) {
        this.exercises = exercises;
        this.attempts = attempts;
        this.sessions = sessions;
        this.messages = messages;
    }

    public ProgressDto forUser(Long userId, ZoneId zone) {
        Object[] t = attempts.totals(userId).get(0);
        long total = num(t[0]).longValue();
        long correct = num(t[1]).longValue();
        double avg = round(num(t[2]).doubleValue());

        List<SubjectStats> bySubject = attempts.statsBySubject(userId).stream()
                .map(r -> new SubjectStats((String) r[0], num(r[1]).longValue(), num(r[2]).longValue(),
                        round(num(r[3]).doubleValue())))
                .toList();

        LocalDate today = LocalDate.now(zone);
        Set<LocalDate> activeDays = new HashSet<>();
        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (int i = DAYS - 1; i >= 0; i--) perDay.put(today.minusDays(i), 0L);
        for (Instant at : attempts.activityTimes(userId)) {
            LocalDate d = at.atZone(zone).toLocalDate();
            activeDays.add(d);
            perDay.computeIfPresent(d, (k, v) -> v + 1);
        }
        List<DayStats> daily = perDay.entrySet().stream().map(e -> new DayStats(e.getKey(), e.getValue())).toList();

        return new ProgressDto(
                exercises.countByUserId(userId), total, correct,
                total == 0 ? 0 : round((double) correct / total),
                avg,
                sessions.countByUserId(userId), messages.countUserMessages(userId),
                streak(activeDays, today), bySubject, daily);
    }

    private static int streak(Set<LocalDate> days, LocalDate today) {
        LocalDate d = days.contains(today) ? today : today.minusDays(1);
        int n = 0;
        while (days.contains(d)) {
            n++;
            d = d.minusDays(1);
        }
        return n;
    }

    private static Number num(Object o) {
        return o == null ? 0 : (Number) o;
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
