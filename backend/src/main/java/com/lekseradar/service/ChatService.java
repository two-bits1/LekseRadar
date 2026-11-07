package com.lekseradar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.dto.ai.AiMessageRequestDTO;
import com.lekseradar.dto.ai.AiResponseDTO;
import com.lekseradar.dto.chat.*;
import com.lekseradar.exception.ApiException;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.model.ChatMessageEntity;
import com.lekseradar.model.ChatSessionEntity;
import com.lekseradar.model.SubjectEntity;
import com.lekseradar.model.UserEntity;
import com.lekseradar.model.enums.ChatModeEnum;
import com.lekseradar.model.enums.MessageRoleEnum;
import com.lekseradar.repository.AssignmentRepository;
import com.lekseradar.repository.ChatMessageRepository;
import com.lekseradar.repository.ChatSessionRepository;
import com.lekseradar.repository.MaterialRepository;
import com.lekseradar.repository.SubjectRepository;
import com.lekseradar.service.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ИИ-чат. Режим, промпт и данные для промпта берутся из базы:
 * start_chat → chat_system_prompt + chat_prompt_vars (+ materials_for_prompt) → ответ ИИ.
 */
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final int TITLE_MAX_LENGTH = 60;
    private static final int MATERIALS_LIMIT = 5;
    /** Сколько вопросов просить у ИИ в режиме quiz ({{count}} в промпте). */
    private static final int QUIZ_QUESTION_COUNT = 10;
    /** Тьютор ставит эту метку, когда ученик выбрал язык. */
    private static final Pattern LANGUAGE_TAG = Pattern.compile("\\[\\[LANGUAGE=([A-Za-z]{2,3})]]");
    private static final Pattern LANGUAGE_QUESTION_TAG = Pattern.compile("\\[\\[LANGUAGE_QUESTION]]");
    private static final Pattern PROMPT_VAR = Pattern.compile("\\{\\{(\\w+)}}");

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final SubjectRepository subjects;
    private final AssignmentRepository assignments;
    private final MaterialRepository materials;
    private final AiService ai;
    private final ObjectMapper mapper;

    public ChatService(ChatSessionRepository sessions, ChatMessageRepository messages, SubjectRepository subjects,
                       AssignmentRepository assignments, MaterialRepository materials, AiService ai,
                       ObjectMapper mapper) {
        this.sessions = sessions;
        this.messages = messages;
        this.subjects = subjects;
        this.assignments = assignments;
        this.materials = materials;
        this.ai = ai;
        this.mapper = mapper;
    }

    /** @param archived null — все чаты, true/false — только архивные / только активные */
    public List<ChatResponseDTO> list(Long userId, Boolean archived) {
        List<ChatSessionEntity> found = archived == null
                ? sessions.findByUserIdOrderByUpdatedAtDesc(userId)
                : sessions.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, archived);
        Map<Long, String> names = subjects.findAll().stream()
                .collect(Collectors.toMap(SubjectEntity::getId, SubjectEntity::getName));
        return found.stream().map(s -> ChatResponseDTO.from(s, names.get(s.getSubjectId()))).toList();
    }

    public ChatResponseDTO get(Long userId, Long chatId) {
        return toResponse(owned(userId, chatId));
    }

    /** Чат создаёт функция базы start_chat: она же находит или создаёт предмет и пишет событие chat_started. */
    @Transactional
    public ChatResponseDTO create(Long userId, CreateChatRequestDTO req) {
        if (req == null) req = new CreateChatRequestDTO(null, null, null, null, null, null);
        if (req.assignmentId() != null) {
            assignments.findByIdAndUserId(req.assignmentId(), userId)
                    .orElseThrow(() -> new NotFoundException("Домашнее задание не найдено"));
        }
        Long id = sessions.startChat(userId, req.modeOrDefault().value(), blankToNull(req.subject()),
                blankToNull(req.topic()), blankToNull(req.title()), req.assignmentId());
        ChatSessionEntity s = owned(userId, id);
        if (req.level() != null) {
            s.setLevel(req.level());
        }
        return toResponse(s);
    }

    @Transactional
    public ChatResponseDTO update(Long userId, Long chatId, UpdateChatRequestDTO req) {
        ChatSessionEntity s = owned(userId, chatId);
        if (!isBlank(req.title())) s.setTitle(req.title().trim());
        if (req.topic() != null) s.setTopic(blankToNull(req.topic()));
        if (req.archived() != null) s.setArchived(req.archived());
        s.touch();
        return toResponse(s);
    }

    /** Сообщения удаляются базой (ON DELETE CASCADE). */
    @Transactional
    public void delete(Long userId, Long chatId) {
        sessions.delete(owned(userId, chatId));
    }

    public List<MessageResponseDTO> messages(Long userId, Long chatId) {
        owned(userId, chatId);
        return messages.findBySessionIdOrderByCreatedAtAscIdAsc(chatId).stream().map(MessageResponseDTO::from).toList();
    }

    /**
     * Вопрос → ИИ → ответ. Промпт режима чата берётся из базы и заполняется данными чата
     * (язык, предмет, тема, ДЗ, слабые темы, учебные материалы). Вызов ИИ идёт вне транзакции,
     * чтобы не держать соединение с БД. Если ИИ недоступен, вопрос остаётся в истории.
     */
    public ChatReplyResponseDTO send(UserEntity user, Long chatId, SendMessageRequestDTO req) {
        String content = req.content().strip();
        ChatSessionEntity s = owned(user.getId(), chatId);
        ChatSessionRepository.SystemPrompt system = loadSystemPrompt(chatId);

        ChatMessageEntity question = messages.save(ChatMessageEntity.fromStudent(chatId, content));

        List<AiMessageRequestDTO> prompt = new ArrayList<>();
        prompt.add(AiMessageRequestDTO.system(render(system.getContent(), promptVars(s, user, content))));
        for (ChatMessageRepository.ContextMessage m : messages.findContext(chatId)) {
            prompt.add(MessageRoleEnum.TUTOR.value().equals(m.getRole())
                    ? AiMessageRequestDTO.assistant(m.getContent())
                    : AiMessageRequestDTO.user(m.getContent()));
        }

        AiResponseDTO reply = ai.complete(prompt);
        String text = handleTags(chatId, reply.content());

        ChatMessageEntity answer = ChatMessageEntity.fromTutor(chatId, limit(text), reply.model(), reply.tokensUsed());
        answer.setPromptId(system.getPromptId());
        answer = messages.save(answer);

        // перечитываем: язык мог поменяться в set_chat_language
        ChatSessionEntity fresh = owned(user.getId(), chatId);
        if (ChatSessionEntity.DEFAULT_TITLE.equals(fresh.getTitle())) {
            fresh.setTitle(makeTitle(content));
        }
        fresh.touch();
        sessions.save(fresh);

        return new ChatReplyResponseDTO(toResponse(fresh), MessageResponseDTO.from(question), MessageResponseDTO.from(answer));
    }

    private ChatSessionRepository.SystemPrompt loadSystemPrompt(Long chatId) {
        try {
            ChatSessionRepository.SystemPrompt p = sessions.systemPrompt(chatId);
            if (p == null || p.getContent() == null) throw new IllegalStateException("empty prompt");
            return p;
        } catch (DataAccessException | IllegalStateException e) {
            log.error("Нет промпта для чата {}: {}", chatId, e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Промпт для чата не загружен в базу: запустите ai-content/load/load.py");
        }
    }

    /** Значения для {{...}}: из chat_prompt_vars, плюс {{materials}} и {{count}}. */
    private Map<String, String> promptVars(ChatSessionEntity s, UserEntity user, String question) {
        Map<String, String> vars = new HashMap<>();
        try {
            Map<String, Object> raw = mapper.readValue(sessions.promptVars(s.getId()), new TypeReference<>() {});
            raw.forEach((k, v) -> vars.put(k, v == null ? "" : String.valueOf(v)));
        } catch (Exception e) {
            log.warn("chat_prompt_vars({}) не разобран: {}", s.getId(), e.getMessage());
        }
        String found = materials.forPrompt(user.getId(), question, s.getSubjectId(), MATERIALS_LIMIT, s.getTopic());
        vars.put("materials", isBlank(found) ? "нет материалов" : found);
        if (s.getMode() == ChatModeEnum.QUIZ) {
            vars.putIfAbsent("count", String.valueOf(QUIZ_QUESTION_COUNT));
        }
        return vars;
    }

    /** Подставляет {{key}}; неизвестные переменные остаются как есть. */
    static String render(String template, Map<String, String> vars) {
        Matcher m = PROMPT_VAR.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = vars.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** [[LANGUAGE=xx]] → сохранить язык чата; служебные метки ученику не показываем. */
    private String handleTags(Long chatId, String text) {
        Matcher lang = LANGUAGE_TAG.matcher(text);
        if (lang.find()) {
            sessions.setLanguage(chatId, lang.group(1).toLowerCase(Locale.ROOT));
        }
        String cleaned = LANGUAGE_QUESTION_TAG.matcher(LANGUAGE_TAG.matcher(text).replaceAll("")).replaceAll("");
        return cleaned.strip();
    }

    private ChatResponseDTO toResponse(ChatSessionEntity s) {
        String subject = s.getSubjectId() == null ? null
                : subjects.findById(s.getSubjectId()).map(SubjectEntity::getName).orElse(null);
        return ChatResponseDTO.from(s, subject);
    }

    /** Чат пользователя; чужой чат выглядит как несуществующий (404), чтобы не раскрывать id. */
    private ChatSessionEntity owned(Long userId, Long chatId) {
        return sessions.findByIdAndUserId(chatId, userId).orElseThrow(() -> new NotFoundException("Чат не найден"));
    }

    /** В базе CHECK length(content) <= 8000. */
    private static String limit(String text) {
        int max = ChatMessageEntity.MAX_CONTENT_LENGTH;
        if (text.isEmpty()) return "…";
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
