package com.lekseradar.chat;

import com.lekseradar.auth.CurrentUser;
import com.lekseradar.chat.ChatDtos.*;
import com.lekseradar.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    public List<SessionDto> list(@CurrentUser User user) {
        return chat.list(user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDto create(@CurrentUser User user, @Valid @RequestBody(required = false) CreateSessionRequest req) {
        return chat.create(user.getId(), req);
    }

    @PatchMapping("/{id}")
    public SessionDto update(@CurrentUser User user, @PathVariable Long id, @Valid @RequestBody UpdateSessionRequest req) {
        return chat.update(user.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable Long id) {
        chat.delete(user.getId(), id);
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> messages(@CurrentUser User user, @PathVariable Long id) {
        return chat.messages(user.getId(), id);
    }

    @PostMapping("/{id}/messages")
    public ExchangeDto send(@CurrentUser User user, @PathVariable Long id, @Valid @RequestBody SendMessageRequest req) {
        return chat.send(user.getId(), id, req.content().strip());
    }
}
