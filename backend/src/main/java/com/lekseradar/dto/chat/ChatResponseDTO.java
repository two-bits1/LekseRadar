package com.lekseradar.dto.chat;

import com.lekseradar.model.ChatSessionEntity;
import com.lekseradar.model.enums.ChatModeEnum;
import com.lekseradar.model.enums.ExerciseLevelEnum;

import java.time.Instant;

/**
 * Чат.
 *
 * @param subject   название предмета или null
 * @param language  язык ответов ИИ; null — тьютор сначала спросит, на каком языке учиться
 * @param quizSetId только у чата quiz: какой набор вопросов он создал
 */
public record ChatResponseDTO(Long id, String title, String topic, ChatModeEnum mode, String subject, String language,
                           ExerciseLevelEnum level, Long assignmentId, Long quizSetId, boolean archived,
                           Instant createdAt, Instant updatedAt) {

    /** @param subject название предмета (subjects.name) или null */
    public static ChatResponseDTO from(ChatSessionEntity s, String subject) {
        return new ChatResponseDTO(s.getId(), s.getTitle(), s.getTopic(), s.getMode(), subject, s.getLanguage(),
                s.getLevel(), s.getAssignmentId(), s.getQuizSetId(), s.isArchived(), s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
