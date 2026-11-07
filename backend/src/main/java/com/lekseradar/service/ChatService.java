package com.lekseradar.service;

import com.lekseradar.dto.chat.*;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.model.ChatMessage;
import com.lekseradar.model.ChatSession;
import com.lekseradar.model.MessageRole;
import com.lekseradar.model.User;
import com.lekseradar.repository.ChatMessageRepository;
import com.lekseradar.repository.ChatSessionRepository;
import com.lekseradar.repository.UserEventRepository;
import com.lekseradar.service.ai.AiClient;
import com.lekseradar.service.ai.AiMessage;
import com.lekseradar.service.ai.AiReply;
import com.lekseradar.service.ai.Prompts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {
    private static final int TITLE_MAX_LENGTH = 60;

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final UserEventRepository events;
    private final AiClient ai;

    public ChatService(ChatSessionRepository sessions, ChatMessageRepository messages, UserEventRepository events,
                       AiClient ai) {
        this.sessions = sessions;
        this.messages = messages;
        this.events = events;
        this.ai = ai;
    }

    /** @param archived null — все чаты, true/false — только архивные / только активные */
    public List<ChatResponse> list(Long userId, Boolean archived) {
        List<ChatSession> found = archived == null
                ? sessions.findByUserIdOrderByUpdatedAtDesc(userId)
                : sessions.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, archived);
        return found.stream().map(ChatResponse::from).toList();
    }

    public ChatResponse get(Long userId, Long chatId) {
        return ChatResponse.from(owned(userId, chatId));
    }

    @Transactional
    public ChatResponse create(Long userId, CreateChatRequest req) {
        String title = req == null || isBlank(req.title()) ? ChatSession.DEFAULT_TITLE : req.title().trim();
        String topic = req == null ? null : blankToNull(req.topic());
        ChatSession s = sessions.save(new ChatSession(userId, title, topic));
        events.add(userId, "chat_started", "chat_sessions", s.getId());
        return ChatResponse.from(s);
    }

    @Transactional
    public ChatResponse update(Long userId, Long chatId, UpdateChatRequest req) {
        ChatSession s = owned(userId, chatId);
        if (!isBlank(req.title())) s.setTitle(req.title().trim());
        if (req.topic() != null) s.setTopic(blankToNull(req.topic()));
        if (req.archived() != null) s.setArchived(req.archived());
        s.touch();
        return ChatResponse.from(s);
    }

    /** Сообщения удаляются базой (ON DELETE CASCADE). */
    @Transactional
    public void delete(Long userId, Long chatId) {
        sessions.delete(owned(userId, chatId));
    }

    public List<MessageResponse> messages(Long userId, Long chatId) {
        owned(userId, chatId);
        return messages.findBySessionIdOrderByCreatedAtAscIdAsc(chatId).stream().map(MessageResponse::from).toList();
    }

    /**
     * Сохраняет вопрос, берёт историю из вида chat_context, спрашивает ИИ и сохраняет ответ.
     * Вызов ИИ идёт вне транзакции, чтобы не держать соединение с БД во время запроса к OpenAI.
     * Если ИИ недоступен, вопрос остаётся в истории, и его можно задать снова.
     */
    public ChatReplyResponse send(User user, Long chatId, SendMessageRequest req) {
        String content = req.content().strip();
        ChatSession s = owned(user.getId(), chatId);

        ChatMessage question = messages.save(ChatMessage.fromStudent(chatId, content));

        List<AiMessage> prompt = new ArrayList<>();
        prompt.add(AiMessage.system(Prompts.tutor(s.getTopic(), user.getLocale())));
        for (ChatMessageRepository.ContextMessage m : messages.findContext(chatId)) {
            prompt.add(MessageRole.TUTOR.value().equals(m.getRole())
                    ? AiMessage.assistant(m.getContent())
                    : AiMessage.user(m.getContent()));
        }

        AiReply reply = ai.complete(prompt);
        ChatMessage answer = messages.save(ChatMessage.fromTutor(chatId, limit(reply.content()), reply.model(),
                reply.tokensUsed()));

        if (ChatSession.DEFAULT_TITLE.equals(s.getTitle())) {
            s.setTitle(makeTitle(content));
        }
        s.touch();
        sessions.save(s);

        return new ChatReplyResponse(ChatResponse.from(s), MessageResponse.from(question),
                MessageResponse.from(answer));
    }

    /** Чат пользователя; чужой чат выглядит как несуществующий (404), чтобы не раскрывать id. */
    private ChatSession owned(Long userId, Long chatId) {
        return sessions.findByIdAndUserId(chatId, userId).orElseThrow(() -> new NotFoundException("Чат не найден"));
    }

    /** В базе CHECK length(content) <= 8000. */
    private static String limit(String text) {
        int max = ChatMessage.MAX_CONTENT_LENGTH;
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static String makeTitle(String text) {
        String t = text.strip().replaceAll("\\s+", " ");
        return t.length() > TITLE_MAX_LENGTH ? t.substring(0, TITLE_MAX_LENGTH) + "…" : t;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
