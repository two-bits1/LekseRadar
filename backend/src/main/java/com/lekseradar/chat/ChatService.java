package com.lekseradar.chat;

import com.lekseradar.ai.AiClient;
import com.lekseradar.ai.AiMessage;
import com.lekseradar.ai.Prompts;
import com.lekseradar.chat.ChatDtos.*;
import com.lekseradar.common.ApiException;
import com.lekseradar.config.AppProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {
    private static final String DEFAULT_TITLE = "Новый чат";

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final AiClient ai;
    private final int historyLimit;

    public ChatService(ChatSessionRepository sessions, ChatMessageRepository messages, AiClient ai,
                       AppProperties props) {
        this.sessions = sessions;
        this.messages = messages;
        this.ai = ai;
        this.historyLimit = props.openai().chatHistoryLimit();
    }

    public List<SessionDto> list(Long userId) {
        return sessions.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(SessionDto::from).toList();
    }

    @Transactional
    public SessionDto create(Long userId, CreateSessionRequest req) {
        String title = req == null || req.title() == null || req.title().isBlank() ? DEFAULT_TITLE : req.title().trim();
        String subject = req == null ? null : blankToNull(req.subject());
        return SessionDto.from(sessions.save(new ChatSession(userId, title, subject)));
    }

    @Transactional
    public SessionDto update(Long userId, Long sessionId, UpdateSessionRequest req) {
        ChatSession s = owned(userId, sessionId);
        if (req.title() != null && !req.title().isBlank()) s.setTitle(req.title().trim());
        if (req.subject() != null) s.setSubject(blankToNull(req.subject()));
        return SessionDto.from(s);
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        ChatSession s = owned(userId, sessionId);
        messages.deleteBySessionId(s.getId());
        sessions.delete(s);
    }

    public List<MessageDto> messages(Long userId, Long sessionId) {
        owned(userId, sessionId);
        return messages.findBySessionIdOrderByCreatedAtAscIdAsc(sessionId).stream().map(MessageDto::from).toList();
    }

    /**
     * Сохраняет вопрос, отправляет историю в ИИ и сохраняет ответ.
     * Вызов ИИ идёт вне транзакции, чтобы не держать соединение с БД во время запроса к OpenAI.
     */
    public ExchangeDto send(Long userId, Long sessionId, String content) {
        ChatSession s = owned(userId, sessionId);

        List<ChatMessage> recent = new ArrayList<>(
                messages.findBySessionIdOrderByCreatedAtDescIdDesc(sessionId, PageRequest.of(0, historyLimit)));
        Collections.reverse(recent);

        List<AiMessage> prompt = new ArrayList<>();
        prompt.add(AiMessage.system(Prompts.tutor(s.getSubject())));
        for (ChatMessage m : recent) {
            prompt.add(m.getRole() == ChatMessage.Role.USER ? AiMessage.user(m.getContent())
                    : AiMessage.assistant(m.getContent()));
        }
        prompt.add(AiMessage.user(content));

        String answer = ai.complete(prompt);

        ChatMessage userMsg = messages.save(new ChatMessage(sessionId, ChatMessage.Role.USER, content));
        ChatMessage aiMsg = messages.save(new ChatMessage(sessionId, ChatMessage.Role.ASSISTANT, answer));
        if (DEFAULT_TITLE.equals(s.getTitle())) {
            s.setTitle(makeTitle(content));
        }
        s.touch();
        sessions.save(s);

        return new ExchangeDto(SessionDto.from(s), MessageDto.from(userMsg), MessageDto.from(aiMsg));
    }

    private ChatSession owned(Long userId, Long sessionId) {
        return sessions.findByIdAndUserId(sessionId, userId).orElseThrow(() -> ApiException.notFound("Чат"));
    }

    private static String makeTitle(String text) {
        String t = text.strip().replaceAll("\\s+", " ");
        return t.length() > 60 ? t.substring(0, 60) + "…" : t;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
