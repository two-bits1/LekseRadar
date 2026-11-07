package com.lekseradar.config;

import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Лимит запросов, которые могут вызвать ИИ, на одного ученика. Без него один аккаунт (или украденный токен)
 * за минуты съедает дневной лимит ключа Groq/OpenAI для всех.
 * Вопрос в чат, новый квиз, карточки от ИИ — 30 за 10 минут; ответы в квизе (текстовые проверяет ИИ) — 120 за 10 минут.
 */
@Configuration
public class AiRateLimitConfig implements WebMvcConfigurer {
    static final Duration WINDOW = Duration.ofMinutes(10);
    static final int GENERATE_LIMIT = 30;
    static final int ANSWER_LIMIT = 120;

    private final RateLimiter limiter;

    public AiRateLimitConfig(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new Limit("ai", GENERATE_LIMIT))
                .addPathPatterns("/api/v1/chats/*/messages", "/api/v1/quizzes", "/api/v1/flashcards/generate");
        registry.addInterceptor(new Limit("answer", ANSWER_LIMIT))
                .addPathPatterns("/api/v1/quizzes/*/answers");
    }

    private class Limit implements HandlerInterceptor {
        private final String name;
        private final int max;

        Limit(String name, int max) {
            this.name = name;
            this.max = max;
        }

        @Override
        public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
            if (!"POST".equals(req.getMethod())) return true;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof UserEntity user)) return true;
            if (!limiter.tryAcquire(name + ":user:" + user.getId(), max, WINDOW)) {
                res.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
                throw new TooManyRequestsException("Слишком много запросов к ИИ. Подождите несколько минут");
            }
            return true;
        }
    }
}
