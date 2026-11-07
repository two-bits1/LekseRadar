package com.lekseradar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void fullFlow() throws Exception {
        // Регистрация и вход
        String token = json(post("/api/auth/register"), null,
                "{\"email\":\"Ann@Example.com\",\"password\":\"secret123\",\"displayName\":\"Аня\"}", 201)
                .path("token").asText();
        json(post("/api/auth/register"), null,
                "{\"email\":\"ann@example.com\",\"password\":\"secret123\",\"displayName\":\"X\"}", 409);
        json(post("/api/auth/login"), null, "{\"email\":\"ann@example.com\",\"password\":\"wrong\"}", 401);
        String token2 = json(post("/api/auth/login"), null,
                "{\"email\":\"ann@example.com\",\"password\":\"secret123\"}", 200).path("token").asText();
        assertThat(json(get("/api/users/me"), token2, null, 200).path("displayName").asText()).isEqualTo("Аня");

        // Чат
        long chatId = json(post("/api/chats"), token, "{\"subject\":\"Математика\"}", 201).path("id").asLong();
        JsonNode ex = json(post("/api/chats/" + chatId + "/messages"), token,
                "{\"content\":\"Как решать квадратные уравнения?\"}", 200);
        assertThat(ex.path("assistantMessage").path("role").asText()).isEqualTo("ASSISTANT");
        assertThat(ex.path("session").path("title").asText()).isEqualTo("Как решать квадратные уравнения?");
        assertThat(json(get("/api/chats/" + chatId + "/messages"), token, null, 200).size()).isEqualTo(2);

        // Чужой пользователь не видит чат
        String other = json(post("/api/auth/register"), null,
                "{\"email\":\"bob@example.com\",\"password\":\"secret123\",\"displayName\":\"Боб\"}", 201)
                .path("token").asText();
        json(get("/api/chats/" + chatId + "/messages"), other, null, 404);

        // Упражнения
        JsonNode generated = json(post("/api/exercises/generate"), token,
                "{\"subject\":\"Математика\",\"topic\":\"Умножение\",\"difficulty\":\"EASY\",\"count\":2}", 201);
        assertThat(generated.size()).isEqualTo(2);
        JsonNode choice = generated.get(0);
        assertThat(choice.path("type").asText()).isEqualTo("CHOICE");
        assertThat(choice.path("solution").isNull()).isTrue(); // решение скрыто до ответа

        JsonNode answered = json(post("/api/exercises/" + choice.path("id").asLong() + "/answer"), token,
                "{\"answer\":\"56\"}", 200);
        assertThat(answered.path("attempts").get(0).path("correct").asBoolean()).isTrue();
        assertThat(answered.path("solution").asText()).isEqualTo("56");

        json(post("/api/exercises/" + choice.path("id").asLong() + "/answer"), token, "{\"answer\":\"54\"}", 200);
        json(post("/api/exercises/" + generated.get(1).path("id").asLong() + "/answer"), token,
                "{\"answer\":\"Ячейка памяти с именем\"}", 200);

        assertThat(json(get("/api/exercises?subject=Математика"), token, null, 200).path("total").asLong())
                .isEqualTo(2);

        // Прогресс
        JsonNode p = json(get("/api/progress?tz=Europe/Moscow"), token, null, 200);
        assertThat(p.path("exercisesGenerated").asLong()).isEqualTo(2);
        assertThat(p.path("attempts").asLong()).isEqualTo(3);
        assertThat(p.path("correctAttempts").asLong()).isEqualTo(2);
        assertThat(p.path("chatQuestions").asLong()).isEqualTo(1);
        assertThat(p.path("streakDays").asInt()).isEqualTo(1);
        assertThat(p.path("bySubject").get(0).path("subject").asText()).isEqualTo("Математика");
        assertThat(p.path("daily").size()).isEqualTo(14);

        // Удаление чата
        json(delete("/api/chats/" + chatId), token, null, 204);
        assertThat(json(get("/api/chats"), token, null, 200).size()).isZero();
    }

    private JsonNode json(MockHttpServletRequestBuilder req, String token, String body, int expected) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        String res = mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return res.isEmpty() ? mapper.nullNode() : mapper.readTree(res);
    }
}
