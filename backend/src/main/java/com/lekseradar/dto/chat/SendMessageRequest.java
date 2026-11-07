package com.lekseradar.dto.chat;

import com.lekseradar.model.ChatMessageEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(@NotBlank @Size(max = ChatMessageEntity.MAX_CONTENT_LENGTH) String content) {}
