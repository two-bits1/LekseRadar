package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.UserRepository;

import java.math.BigDecimal;

/** @param masteryPct освоение темы 0–100 (correct = 1, partial = 0,5) */
public record TopicProgressResponse(String topic, long attempts, BigDecimal masteryPct) {

    public static TopicProgressResponse from(UserRepository.TopicProgressResponse p) {
        return new TopicProgressResponse(p.getTopic(), p.getAttempts() == null ? 0 : p.getAttempts(), p.getMasteryPct());
    }
}
