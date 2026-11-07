package com.lekseradar.dto.chat;

import jakarta.validation.constraints.Size;

/** Оба поля необязательны: без title чат получит название по первому вопросу. */
public record CreateChatRequest(@Size(max = 200) String title, @Size(max = 200) String topic) {}
