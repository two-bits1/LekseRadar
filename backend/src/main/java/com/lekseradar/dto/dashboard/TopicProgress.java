package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.UserRepository;

import java.math.BigDecimal;

/** @param masteryPct освоение темы 0–100 (correct = 1, partial = 0,5) */
public record TopicProgress(String topic, long attempts, BigDecimal masteryPct) {

    public static TopicProgress from(UserRepository.TopicProgress p) {
        return new TopicProgress(p.getTopic(), p.getAttempts() == null ? 0 : p.getAttempts(), p.getMasteryPct());
    }
}
