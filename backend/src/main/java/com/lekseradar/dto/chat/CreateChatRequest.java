package com.lekseradar.dto.chat;

import com.lekseradar.model.enums.ChatMode;
import com.lekseradar.model.enums.ExerciseLevel;
import jakarta.validation.constraints.Size;

/**
 * Новый чат ({@code POST /chats}). Все поля необязательны.
 *
 * @param title        без названия чат назовётся по первому вопросу (или «Ny samtale: тема»)
 * @param mode         tutor (по умолчанию) или quiz; после создания не меняется
 * @param subject      название предмета, например «Matematikk»; создаётся в базе, если его ещё нет
 * @param assignmentId чат по конкретному ДЗ (assignments.id пользователя)
 * @param level        beginner | intermediate (по умолчанию) | advanced
 */
public record CreateChatRequest(
        @Size(max = 200) String title,
        @Size(max = 200) String topic,
        ChatMode mode,
        @Size(max = 100) String subject,
        Long assignmentId,
        ExerciseLevel level) {

    public ChatMode modeOrDefault() {
        return mode == null ? ChatMode.TUTOR : mode;
    }
}
