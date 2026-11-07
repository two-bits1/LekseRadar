package com.lekseradar.dto.chat;

import com.lekseradar.model.ChatSessionEntity;
import com.lekseradar.model.enums.ChatMode;
import com.lekseradar.model.enums.ExerciseLevel;

import java.time.Instant;

/**
 * Чат.
 *
 * @param subject   название предмета или null
 * @param language  язык ответов ИИ; null — тьютор сначала спросит, на каком языке учиться
 * @param quizSetId только у чата quiz: какой набор вопросов он создал
 */
public record ChatResponse(Long id, String title, String topic, ChatMode mode, String subject, String language,
                           ExerciseLevel level, Long assignmentId, Long quizSetId, boolean archived,
                           Instant createdAt, Instant updatedAt) {

    /** @param subject название предмета (subjects.name) или null */
    public static ChatResponse from(ChatSessionEntity s, String subject) {
        return new ChatResponse(s.getId(), s.getTitle(), s.getTopic(), s.getMode(), subject, s.getLanguage(),
                s.getLevel(), s.getAssignmentId(), s.getQuizSetId(), s.isArchived(), s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
