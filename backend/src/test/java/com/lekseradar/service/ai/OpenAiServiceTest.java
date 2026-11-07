package com.lekseradar.service.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Сколько ждать перед повтором, когда ИИ отвечает 429 (лимит в минуту). */
class OpenAiServiceTest {

    private static HttpClientErrorException tooMany(String body, String retryAfter) {
        HttpHeaders h = new HttpHeaders();
        if (retryAfter != null) h.add("retry-after", retryAfter);
        return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", h,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    void waitFromGroqMessage() {
        String body = "{\"error\":{\"message\":\"Rate limit reached ... Please try again in 3.9075s. Need more tokens?\"}}";
        assertEquals(4208, OpenAiService.retryAfterMs(tooMany(body, null)));
    }

    @Test
    void waitFromHeader() {
        assertEquals(2300, OpenAiService.retryAfterMs(tooMany("{}", "2")));
    }

    @Test
    void noRetryForLongWaitOrUnknown() {
        assertEquals(-1, OpenAiService.retryAfterMs(tooMany("Please try again in 7m12s", null)));
        assertEquals(-1, OpenAiService.retryAfterMs(tooMany("Please try again in 45s", null)));
        assertEquals(-1, OpenAiService.retryAfterMs(tooMany("{}", null)));
    }
}
