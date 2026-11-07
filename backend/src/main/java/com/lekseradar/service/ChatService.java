package com.lekseradar.service;

import com.lekseradar.dto.ai.AiMessageRequestDTO;
import com.lekseradar.dto.ai.AiResponseDTO;
import com.lekseradar.dto.chat.*;
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
import com.lekseradar.repository.SubjectRepository;
import com.lekseradar.service.ai.AiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ИИ-чат двух видов (режим задаётся при создании):
 * <ul>
 *   <li>{@code tutor} — репетитор отвечает текстом по промпту tutor;</li>
 *   <li>{@code quiz} — каждое сообщение ученика («квиз по дробям», «сложнее») → ИИ составляет квиз
 *       по промпту quiz_generator, в ответе чата {@code chat.quizSetId}.</li>
 * </ul>
 * Промпты берутся из базы ({@link PromptService}), туда их кладёт ai-content/load/load.py.
 */
@Service
public class ChatService {
    private static final int TITLE_MAX_LENGTH = 60;
    /** Тьютор ставит эту метку, когда ученик выбрал язык. */
    private static final Pattern LANGUAGE_TAG = Pattern.compile("\\[\\[LANGUAGE=([A-Za-z]{2,3})]]");
    private static final Pattern LANGUAGE_QUESTION_TAG = Pattern.compile("\\[\\[LANGUAGE_QUESTION]]");

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final SubjectRepository subjects;
    private final AssignmentRepository assignments;
    private final PromptService prompts;
    private final QuizGeneratorService quizGenerator;
    private final AiService ai;

    public ChatService(ChatSessionRepository sessions, ChatMessageRepository messages, SubjectRepository subjects,
                       AssignmentRepository assignments, PromptService prompts, QuizGeneratorService quizGenerator,
                       AiService ai) {
        this.sessions = sessions;
        this.messages = messages;
        this.subjects = subjects;
        this.assignments = assignments;
        this.prompts = prompts;
        this.quizGenerator = quizGenerator;
        this.ai = ai;
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
     * Сообщение ученика → ИИ → ответ. Вызов ИИ идёт вне транзакции, чтобы не держать соединение с БД.
     * Если ИИ недоступен, вопрос остаётся в истории.
     */
    public ChatReplyResponseDTO send(UserEntity user, Long chatId, SendMessageRequestDTO req) {
        String content = req.content().strip();
        ChatSessionEntity s = owned(user.getId(), chatId);
        ChatSessionRepository.SystemPrompt system = prompts.forChat(chatId);   // нет промпта → 503 до сохранения

        ChatMessageEntity question = messages.save(ChatMessageEntity.fromStudent(chatId, content));
        ChatMessageEntity answer = s.getMode() == ChatModeEnum.QUIZ
                ? quizGenerator.generate(user, s, content, null).message()
                : tutorReply(user, s, system, content);

        // перечитываем: язык или квиз чата мог поменяться
        ChatSessionEntity fresh = owned(user.getId(), chatId);
        if (ChatSessionEntity.DEFAULT_TITLE.equals(fresh.getTitle())) {
            fresh.setTitle(makeTitle(content));
        }
        fresh.touch();
        sessions.save(fresh);

        return new ChatReplyResponseDTO(toResponse(fresh), MessageResponseDTO.from(question), MessageResponseDTO.from(answer));
    }

    /** Промпт tutor + {{переменные}} + последние 10 реплик (chat_context). */
    private ChatMessageEntity tutorReply(UserEntity user, ChatSessionEntity s,
                                         ChatSessionRepository.SystemPrompt system, String content) {
        List<AiMessageRequestDTO> prompt = new ArrayList<>();
        prompt.add(AiMessageRequestDTO.system(PromptService.render(system.getContent(),
                prompts.chatVars(s, user.getId(), content))));
        for (ChatMessageRepository.ContextMessage m : messages.findContext(s.getId())) {
            prompt.add(MessageRoleEnum.TUTOR.value().equals(m.getRole())
                    ? AiMessageRequestDTO.assistant(m.getContent())
                    : AiMessageRequestDTO.user(m.getContent()));
        }

        AiResponseDTO reply = ai.complete(prompt);
        String text = handleTags(s.getId(), reply.content());

        ChatMessageEntity answer = ChatMessageEntity.fromTutor(s.getId(), limit(text), reply.model(), reply.tokensUsed());
        answer.setPromptId(system.getPromptId());
        return messages.save(answer);
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
