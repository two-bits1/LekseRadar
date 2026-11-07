package com.lekseradar.service;

import com.lekseradar.dto.dashboard.DashboardResponseDTO;
import com.lekseradar.dto.dashboard.DayActivityResponseDTO;
import com.lekseradar.dto.dashboard.EventResponseDTO;
import com.lekseradar.dto.dashboard.FlashcardStatsResponseDTO;
import com.lekseradar.dto.dashboard.TopicProgressResponseDTO;
import com.lekseradar.repository.FlashcardRepository;
import com.lekseradar.repository.UserEventRepository;
import com.lekseradar.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/** Цифры для главной и экрана прогресса (виды user_dashboard, user_topic_progress, user_daily_activity) и лента действий. */
@Service
public class DashboardService {
    static final int MAX_HISTORY = 200;

    private final UserRepository users;
    private final FlashcardRepository cards;
    private final UserEventRepository events;

    public DashboardService(UserRepository users, FlashcardRepository cards, UserEventRepository events) {
        this.users = users;
        this.cards = cards;
        this.events = events;
    }

    public DashboardResponseDTO dashboard(Long userId) {
        FlashcardStatsResponseDTO flashcards = cards.findStats(userId)
                .map(FlashcardStatsResponseDTO::from).orElse(FlashcardStatsResponseDTO.EMPTY);
        List<TopicProgressResponseDTO> topics = users.findTopicProgress(userId).stream()
                .map(TopicProgressResponseDTO::from).toList();
        List<DayActivityResponseDTO> days = users.findDailyActivity(userId).stream()
                .map(DayActivityResponseDTO::from).toList();
        return DashboardResponseDTO.from(users.findDashboard(userId).orElse(null), flashcards, topics, days);
    }

    /** Новые сверху. */
    public List<EventResponseDTO> history(Long userId, int limit) {
        int size = Math.max(1, Math.min(limit, MAX_HISTORY));
        return events.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, size)).stream()
                .map(EventResponseDTO::from).toList();
    }
}
