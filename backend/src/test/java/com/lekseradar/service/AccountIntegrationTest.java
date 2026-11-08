package com.lekseradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.TestUsers;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.service.collector.CollectorClient;
import com.lekseradar.service.collector.CollectorClient.CollectResult;
import com.lekseradar.service.collector.CollectorClient.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

/** GDPR: выгрузка данных и удаление аккаунта на настоящей базе (TEST_DB_URL, V1–V27). Транзакция откатывается. */
@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DB_URL}",
        "spring.datasource.username=${TEST_DB_USER:lekseradar}",
        "spring.datasource.password=${TEST_DB_PASSWORD:}",
        "app.jwt.secret=integration-test-secret-integration-test",
        "app.openai.api-key=sk-not-used",
        "app.credentials.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.sync.schedule-enabled=false",
        "app.reminders.enabled=false",
})
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@Transactional
class AccountIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @MockitoBean CollectorClient collector;
    @MockitoBean MailService mail;
    @Autowired AuthService auth;
    @Autowired PlatformService platforms;
    @Autowired AccountService account;
    @Autowired JdbcTemplate jdbc;

    private Long userWithData(String email) throws Exception {
        Long id = TestUsers.register(auth, mail, email, "password123", "Ola").user().id();
        when(collector.collect(anyMap())).thenReturn(new CollectResult(
                JSON.readTree("[{\"source\": \"canvas\", \"id\": \"c-1\", \"title\": \"Oblig 1\", \"status\": \"todo\"}]"),
                Map.of("canvas", new Source(true, 1, null, false)), Map.of()));
        platforms.connect(id, PlatformEnum.CANVAS,
                JSON.readTree("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"secret-canvas-token\"}"));
        Long chat = jdbc.queryForObject("INSERT INTO chat_sessions (user_id, title) VALUES (?, 'Brøk') RETURNING id", Long.class, id);
        jdbc.update("INSERT INTO chat_messages (session_id, role, content) VALUES (?, 'student', 'Hva er 1/2?')", chat);
        return id;
    }

    @Test
    void exportHasEverythingButNoSecrets() throws Exception {
        String email = "export-" + System.nanoTime() + "@example.com";
        Long id = userWithData(email);

        String raw = account.export(id);
        JsonNode data = JSON.readTree(raw);

        assertThat(data.path("profile").path("email").asText()).isEqualTo(email);
        assertThat(data.path("profile").has("password_hash")).isFalse();
        assertThat(data.path("profile").path("terms_version").asText()).isEqualTo(AuthService.TERMS_VERSION);
        assertThat(data.path("platforms").get(0).has("credentials_enc")).isFalse();
        assertThat(data.path("assignments").get(0).path("title").asText()).isEqualTo("Oblig 1");
        assertThat(data.path("chats").get(0).path("messages").get(0).path("content").asText()).isEqualTo("Hva er 1/2?");
        assertThat(raw).doesNotContain("secret-canvas-token");
    }

    @Test
    void deleteNeedsPasswordAndRemovesEverything() throws Exception {
        Long id = userWithData("delete-" + System.nanoTime() + "@example.com");

        assertThatThrownBy(() -> account.delete(id, "wrong-password")).isInstanceOf(BadRequestException.class);
        account.delete(id, "password123");

        for (String table : new String[]{"users WHERE id", "platform_connections WHERE user_id", "assignments WHERE user_id",
                "chat_sessions WHERE user_id", "user_events WHERE user_id"}) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " = ?", Integer.class, id)).as(table).isZero();
        }
    }
}
