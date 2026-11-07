package com.lekseradar.dto.chat;

import com.lekseradar.model.enums.ChatModeEnum;
import com.lekseradar.model.enums.ExerciseLevelEnum;
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
public record CreateChatRequestDTO(
        @Size(max = 200) String title,
        @Size(max = 200) String topic,
        ChatModeEnum mode,
        @Size(max = 100) String subject,
        Long assignmentId,
        ExerciseLevelEnum level) {

    public ChatModeEnum modeOrDefault() {
        return mode == null ? ChatModeEnum.TUTOR : mode;
    }
}
