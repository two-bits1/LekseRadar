package com.lekseradar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.exception.ApiException;
import com.lekseradar.model.ChatSessionEntity;
import com.lekseradar.repository.AiPromptRepository;
import com.lekseradar.repository.ChatSessionRepository;
import com.lekseradar.repository.MaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Промпты ИИ. Тексты лежат в GitHub (ai-content/prompts/*.md), скрипт ai-content/load/load.py
 * кладёт их в таблицу ai_prompts, а бэкенд берёт отсюда активную версию и подставляет {{переменные}}.
 */
@Service
public class PromptService {
    private static final Logger log = LoggerFactory.getLogger(PromptService.class);

    public static final String TUTOR = "tutor";
    public static final String QUIZ_GENERATOR = "quiz_generator";
    public static final String ANSWER_CHECKER = "answer_checker";

    private static final int MATERIALS_LIMIT = 5;
    private static final Pattern PROMPT_VAR = Pattern.compile("\\{\\{(\\w+)}}");

    private final ChatSessionRepository sessions;
    private final AiPromptRepository prompts;
    private final MaterialRepository materials;
    private final ObjectMapper mapper;

    public PromptService(ChatSessionRepository sessions, AiPromptRepository prompts, MaterialRepository materials,
                         ObjectMapper mapper) {
        this.sessions = sessions;
        this.prompts = prompts;
        this.materials = materials;
        this.mapper = mapper;
    }

    /** Промпт режима чата (tutor → tutor.md, quiz → quiz_generator.md). Нет промпта → 503. */
    public ChatSessionRepository.SystemPrompt forChat(Long chatId) {
        try {
            ChatSessionRepository.SystemPrompt p = sessions.systemPrompt(chatId);
            if (p == null || p.getContent() == null) throw new IllegalStateException("empty prompt");
            return p;
        } catch (DataAccessException | IllegalStateException e) {
            log.error("Нет промпта для чата {}: {}", chatId, e.getMessage());
            throw notLoaded();
        }
    }

    /** Активная версия промпта по ключу (имя файла без .md). Нет промпта → 503. */
    public String byKey(String key) {
        String content = prompts.getActiveContent(key);
        if (content == null || content.isBlank()) {
            log.error("Промпт '{}' не загружен в ai_prompts", key);
            throw notLoaded();
        }
        return content;
    }

    /**
     * Значения для {{...}} промпта чата: chat_prompt_vars (язык, предмет, тема, уровень, ДЗ, слабые темы)
     * и {{materials}} — учебные материалы, найденные по запросу.
     */
    public Map<String, String> chatVars(ChatSessionEntity s, Long userId, String query) {
        Map<String, String> vars = new HashMap<>();
        try {
            Map<String, Object> raw = mapper.readValue(sessions.promptVars(s.getId()), new TypeReference<>() {});
            raw.forEach((k, v) -> vars.put(k, v == null ? "" : String.valueOf(v)));
        } catch (Exception e) {
            log.warn("chat_prompt_vars({}) не разобран: {}", s.getId(), e.getMessage());
        }
        String found = materials.forPrompt(userId, query, s.getSubjectId(), MATERIALS_LIMIT, s.getTopic());
        vars.put("materials", found == null || found.isBlank() ? "нет материалов" : found);
        return vars;
    }

    /** Подставляет {{key}}; неизвестные переменные остаются как есть. */
    public static String render(String template, Map<String, String> vars) {
        Matcher m = PROMPT_VAR.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = vars.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static ApiException notLoaded() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Промпты ИИ не загружены в базу: запустите ai-content/load/load.py");
    }
}
