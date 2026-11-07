package com.lekseradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lekseradar.dto.ai.AiMessageRequestDTO;
import com.lekseradar.dto.ai.AiResponseDTO;
import com.lekseradar.dto.quiz.StartQuizRequestDTO;
import com.lekseradar.exception.AiServiceException;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.model.ChatMessageEntity;
import com.lekseradar.model.ChatSessionEntity;
import com.lekseradar.model.UserEntity;
import com.lekseradar.model.enums.ChatModeEnum;
import com.lekseradar.model.enums.ExerciseLevelEnum;
import com.lekseradar.model.enums.MessageRoleEnum;
import com.lekseradar.repository.AssignmentRepository;
import com.lekseradar.repository.ChatMessageRepository;
import com.lekseradar.repository.ChatSessionRepository;
import com.lekseradar.repository.QuizSetRepository;
import com.lekseradar.service.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ИИ составляет квиз. Один и тот же JSON даёт и квиз, и колоду флеш-карт (V9).
 * <ol>
 *   <li>промпт режима quiz из базы — {@code quiz_generator} (файл ai-content/prompts/quiz_generator.md);</li>
 *   <li>{{...}} — данные чата (chat_prompt_vars), {{materials}}, {{count}};</li>
 *   <li>ответ ИИ — JSON по database/quiz/quiz.schema.json → {@code import_quiz_json};</li>
 *   <li>неверный JSON → один повтор с текстом ошибки, потом 502.</li>
 * </ol>
 * Вызовы ИИ идут вне транзакции, чтобы не держать соединение с базой.
 */
@Service
public class QuizGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(QuizGeneratorService.class);

    public static final int DEFAULT_COUNT = 8;
    public static final int MAX_COUNT = 30;
    private static final int ATTEMPTS = 2;
    private static final Set<String> LEVELS = Set.of("beginner", "intermediate", "advanced");
    private static final Set<String> TYPES = Set.of("single_choice", "multiple_choice", "text");

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final QuizSetRepository quizSets;
    private final AssignmentRepository assignments;
    private final PromptService prompts;
    private final AiService ai;
    private final ObjectMapper mapper;

    public QuizGeneratorService(ChatSessionRepository sessions, ChatMessageRepository messages,
                                QuizSetRepository quizSets, AssignmentRepository assignments,
                                PromptService prompts, AiService ai, ObjectMapper mapper) {
        this.sessions = sessions;
        this.messages = messages;
        this.quizSets = quizSets;
        this.assignments = assignments;
        this.prompts = prompts;
        this.ai = ai;
        this.mapper = mapper;
    }

    /** Новый набор вопросов. {@code message} — ответ ИИ в чате («Квиз готов: …»). */
    public record Generated(Long quizSetId, String title, int questionCount, ChatMessageEntity message) {}

    /**
     * Готовый набор или новый от ИИ.
     *
     * @param quizSetId набор вопросов
     * @param chatId    квиз-чат, в котором ИИ составил набор (null, если взят готовый из базы)
     * @param generated true — составлен ИИ сейчас, false — взят готовый
     */
    public record SetResult(Long quizSetId, Long chatId, boolean generated) {}

    /**
     * Сначала ищет готовый набор (предмет + тема + уровень + язык профиля), чтобы не тратить ИИ.
     * Не нашёл, или квиз по ДЗ, или {@code fresh: true} → новый квиз-чат и генерация.
     */
    public SetResult findOrGenerate(UserEntity user, StartQuizRequestDTO req) {
        if (req.assignmentId() != null) {
            assignments.findByIdAndUserId(req.assignmentId(), user.getId())
                    .orElseThrow(() -> new NotFoundException("Домашнее задание не найдено"));
        }
        ExerciseLevelEnum level = req.levelOrDefault();
        if (req.assignmentId() == null && !Boolean.TRUE.equals(req.fresh())) {
            Long found = quizSets.find(req.subject().strip(), req.topic().strip(), level.value(), user.getLocale());
            if (found != null) {
                return new SetResult(found, null, false);
            }
        }
        Long chatId = sessions.startChat(user.getId(), ChatModeEnum.QUIZ.value(), req.subject().strip(),
                req.topic().strip(), null, req.assignmentId());
        try {
            ChatSessionEntity chat = sessions.findById(chatId).orElseThrow();
            chat.setLevel(level);
            chat = sessions.save(chat);
            Generated g = generate(user, chat, req.topic().strip(), req.count());
            return new SetResult(g.quizSetId(), chatId, true);
        } catch (RuntimeException e) {
            sessions.deleteById(chatId);   // пустой чат без квиза не оставляем
            throw e;
        }
    }

    /**
     * Составить квиз в квиз-чате. История чата (просьбы ученика: «сложнее», «про дроби») идёт в запрос.
     *
     * @param query по чему искать учебные материалы (тема или последнее сообщение ученика)
     * @param count сколько вопросов (null → 8, максимум 30)
     */
    public Generated generate(UserEntity user, ChatSessionEntity chat, String query, Integer count) {
        var system = prompts.forChat(chat.getId());
        Map<String, String> vars = prompts.chatVars(chat, user.getId(), query);
        vars.put("count", String.valueOf(count == null ? DEFAULT_COUNT : Math.min(Math.max(count, 1), MAX_COUNT)));

        List<AiMessageRequestDTO> prompt = new ArrayList<>();
        prompt.add(AiMessageRequestDTO.system(PromptService.render(system.getContent(), vars)));
        for (ChatMessageRepository.ContextMessage m : messages.findContext(chat.getId())) {
            prompt.add(MessageRoleEnum.TUTOR.value().equals(m.getRole())
                    ? AiMessageRequestDTO.assistant(m.getContent())
                    : AiMessageRequestDTO.user(m.getContent()));
        }
        if (prompt.size() == 1 || !"user".equals(prompt.get(prompt.size() - 1).role())) {
            prompt.add(AiMessageRequestDTO.user("Составь квиз по данным выше. Верни только JSON."));
        }

        for (int attempt = 1; ; attempt++) {
            AiResponseDTO reply = ai.completeJson(prompt);
            try {
                ObjectNode quiz = parse(reply.content());
                normalize(quiz, chat, vars, reply.model());
                Long setId = importQuiz(quiz, user.getId());
                quizSets.setPromptIfEmpty(setId, system.getPromptId());
                String title = quiz.path("title").asText();
                int n = quiz.path("questions").size();
                ChatMessageEntity message = saveResult(chat.getId(), setId, title, n, quiz.path("language").asText(),
                        reply, system.getPromptId());
                log.info("Квиз {} ({} вопросов) составлен в чате {} с попытки {}", setId, n, chat.getId(), attempt);
                return new Generated(setId, title, n, message);
            } catch (InvalidQuizException e) {
                log.warn("Чат {}: квиз от ИИ не принят (попытка {}): {}", chat.getId(), attempt, e.getMessage());
                if (attempt >= ATTEMPTS) {
                    throw new AiServiceException("ИИ составил квиз в неверном формате, попробуйте ещё раз");
                }
                prompt.add(AiMessageRequestDTO.assistant(reply.content()));
                prompt.add(AiMessageRequestDTO.user("Ошибка формата: " + e.getMessage()
                        + ". Верни исправленный квиз целиком, только один JSON-объект."));
            }
        }
    }

    /** JSON от ИИ: убираем ``` и текст вокруг объекта. */
    ObjectNode parse(String content) {
        String text = content == null ? "" : content.strip();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new InvalidQuizException("ответ не содержит JSON-объекта");
        }
        try {
            JsonNode node = mapper.readTree(text.substring(start, end + 1));
            if (!(node instanceof ObjectNode obj)) throw new InvalidQuizException("ожидается JSON-объект");
            return obj;
        } catch (InvalidQuizException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidQuizException("невалидный JSON: " + e.getMessage());
        }
    }

    /**
     * Предмет и тема — как у чата (ИИ мог их перевести, а по ним потом ищется готовый набор).
     * Уровень — от ИИ, если допустимый (ученик мог попросить «сложнее»). Язык — всегда язык сайта или чата.
     */
    void normalize(ObjectNode quiz, ChatSessionEntity chat, Map<String, String> vars, String model) {
        String subject = firstNonBlank(vars.get("subject"), quiz.path("subject").asText(""), "Generelt");
        String title = quiz.path("title").asText("").strip();
        String topic = firstNonBlank(chat.getTopic(), quiz.path("topic").asText(""), title, subject);
        quiz.put("subject", clip(subject, 100));
        quiz.put("topic", clip(topic, 100));
        quiz.put("title", clip(firstNonBlank(title, topic), 200));

        String level = quiz.path("level").asText("").strip().toLowerCase();
        quiz.put("level", LEVELS.contains(level) ? level : chat.getLevel().value());

        // Язык задаёт сайт (V17): код из chat_prompt_vars.language_code (V20), а не то, что написал ИИ.
        // По нему ищется готовый квиз (find_quiz_set) и проверяются текстовые ответы.
        String code = vars.getOrDefault("language_code", "").strip().toLowerCase();
        if (!code.matches("[a-z]{2,3}")) {
            String fromAi = quiz.path("language").asText("").strip().toLowerCase();
            code = fromAi.matches("[a-z]{2,3}") ? fromAi : "nb";
        }
        quiz.put("language", code);
        quiz.put("model", model);

        JsonNode questions = quiz.path("questions");
        if (!(questions instanceof ArrayNode list) || list.isEmpty()) {
            throw new InvalidQuizException("questions должен быть непустым массивом");
        }
        while (list.size() > MAX_COUNT) list.remove(list.size() - 1);
        for (JsonNode q : list) {
            if (!(q instanceof ObjectNode question)) throw new InvalidQuizException("вопрос должен быть объектом");
            String type = question.path("type").asText("");
            if (!TYPES.contains(type)) {
                throw new InvalidQuizException("type должен быть single_choice, multiple_choice или text");
            }
            JsonNode answer = question.path("correct_answer");
            // single_choice с ответом-массивом из одного варианта → строка (иначе база не сравнит ответ)
            if ("single_choice".equals(type) && answer.isArray() && answer.size() == 1) {
                question.put("correct_answer", answer.get(0).asText());
            }
            if ("text".equals(type)) {
                question.remove("options");
            }
        }
    }

    /** import_quiz_json сам проверяет формат и бросает «quiz json: …». */
    private Long importQuiz(ObjectNode quiz, Long userId) {
        try {
            return quizSets.importJson(mapper.writeValueAsString(quiz), userId);
        } catch (DataAccessException e) {
            String msg = String.valueOf(e.getMostSpecificCause().getMessage());
            int i = msg.indexOf("quiz json:");
            if (i < 0) throw e;
            String reason = msg.substring(i + "quiz json:".length()).strip();
            int nl = reason.indexOf('\n');
            throw new InvalidQuizException(nl > 0 ? reason.substring(0, nl) : reason);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new InvalidQuizException(e.getMessage());
        }
    }

    /** Привязать набор к чату и написать в чат «Квиз готов». */
    private ChatMessageEntity saveResult(Long chatId, Long setId, String title, int count, String language,
                                         AiResponseDTO reply, Long promptId) {
        ChatSessionEntity chat = sessions.findById(chatId).orElseThrow();
        chat.setQuizSetId(setId);
        if ("Quiz".equals(chat.getTitle())) {
            chat.setTitle(clip("Quiz: " + title, 200));
        }
        chat.touch();
        sessions.save(chat);

        ChatMessageEntity m = ChatMessageEntity.fromTutor(chatId, readyText(language, title, count),
                reply.model(), reply.tokensUsed());
        m.setPromptId(promptId);
        return messages.save(m);
    }

    static String readyText(String language, String title, int count) {
        return switch (language == null ? "" : language) {
            case "ru" -> "Квиз «%s» готов: вопросов — %d.".formatted(title, count);
            case "uk" -> "Квіз «%s» готовий: запитань — %d.".formatted(title, count);
            case "en" -> "Quiz “%s” is ready: %d questions.".formatted(title, count);
            default -> "Quizen «%s» er klar: %d spørsmål.".formatted(title, count);
        };
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.strip();
        }
        return "";
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** JSON от ИИ не подходит под quiz.schema.json. */
    static class InvalidQuizException extends RuntimeException {
        InvalidQuizException(String message) {
            super(message);
        }
    }
}
