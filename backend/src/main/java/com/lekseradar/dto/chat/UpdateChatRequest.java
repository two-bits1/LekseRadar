package com.lekseradar.dto.chat;

import jakarta.validation.constraints.Size;

/** Меняются только переданные поля; topic = "" убирает тему. */
public record UpdateChatRequest(@Size(max = 200) String title, @Size(max = 200) String topic, Boolean archived) {}
