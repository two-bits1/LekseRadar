package com.lekseradar.controller;

import com.lekseradar.dto.chat.*;
import com.lekseradar.model.UserEntity;
import com.lekseradar.security.CurrentUser;
import com.lekseradar.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@Tag(name = "3. Чаты", description = "Диалоги с ИИ-тьютором, история хранится в БД")
public class ChatController {
    private final ChatService chats;

    public ChatController(ChatService chats) {
        this.chats = chats;
    }

    @GetMapping
    @Operation(summary = "Мои чаты", description = "Последние обновлённые сверху")
    public List<ChatResponseDTO> list(@CurrentUser UserEntity user,
                                   @Parameter(description = "true — только архив, false — только активные, не указан — все")
                                   @RequestParam(required = false) Boolean archived) {
        return chats.list(user.getId(), archived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать чат", description = "Тело необязательно")
    public ChatResponseDTO create(@CurrentUser UserEntity user, @Valid @RequestBody(required = false) CreateChatRequestDTO req) {
        return chats.create(user.getId(), req);
    }

    @GetMapping("/{chatId}")
    @Operation(summary = "Один чат")
    public ChatResponseDTO get(@CurrentUser UserEntity user, @PathVariable Long chatId) {
        return chats.get(user.getId(), chatId);
    }

    @PatchMapping("/{chatId}")
    @Operation(summary = "Переименовать, сменить тему, архивировать")
    public ChatResponseDTO update(@CurrentUser UserEntity user, @PathVariable Long chatId,
                               @Valid @RequestBody UpdateChatRequestDTO req) {
        return chats.update(user.getId(), chatId, req);
    }

    @DeleteMapping("/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить чат вместе с сообщениями")
    public void delete(@CurrentUser UserEntity user, @PathVariable Long chatId) {
        chats.delete(user.getId(), chatId);
    }

    @GetMapping("/{chatId}/messages")
    @Operation(summary = "Сообщения чата", description = "Старые сверху")
    public List<MessageResponseDTO> messages(@CurrentUser UserEntity user, @PathVariable Long chatId) {
        return chats.messages(user.getId(), chatId);
    }

    @PostMapping("/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Задать вопрос ИИ", description = "Возвращает сохранённый вопрос и ответ ИИ (Markdown)")
    public ChatReplyResponseDTO send(@CurrentUser UserEntity user, @PathVariable Long chatId,
                                  @Valid @RequestBody SendMessageRequestDTO req) {
        return chats.send(user, chatId, req);
    }
}
