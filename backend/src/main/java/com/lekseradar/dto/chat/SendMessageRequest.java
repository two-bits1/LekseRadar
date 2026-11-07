package com.lekseradar.dto.chat;

import com.lekseradar.model.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(@NotBlank @Size(max = ChatMessage.MAX_CONTENT_LENGTH) String content) {}
