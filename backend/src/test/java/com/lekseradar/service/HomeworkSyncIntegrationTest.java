package com.lekseradar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.TestUsers;
import com.lekseradar.dto.homework.SyncResponseDTO;
import com.lekseradar.model.enums.AssignmentStatusEnum;
import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.repository.AssignmentRepository;
import com.lekseradar.service.collector.CollectorClient;
import com.lekseradar.service.collector.CollectorClient.CollectResult;
import com.lekseradar.service.collector.CollectorClient.Source;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

/**
 * Подключение Canvas и /homework/sync на настоящей базе (нужна V21 sync_assignments), collector замокан.
 * Запуск как RepositoriesIntegrationTest: TEST_DB_URL=… mvn test. Транзакция откатывается.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DB_URL}",
        "spring.datasource.username=${TEST_DB_USER:lekseradar}",
        "spring.datasource.password=${TEST_DB_PASSWORD:lekseradar}",
        "app.jwt.secret=integration-test-secret-integration-test",
        "app.openai.api-key=sk-not-used",
        "app.credentials.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.sync.schedule-enabled=false",
        "app.reminders.enabled=false",
})
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@Transactional
class HomeworkSyncIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ITEMS = """
            [{"source": "canvas", "id": "c-1", "title": "Oblig 1", "course": "IN1000", "due": "2030-01-10T22:59:00Z",
              "status": "todo", "url": "https://uio.instructure.com/courses/1/assignments/1", "description": "Les kap. 1"},
             {"source": "canvas", "id": "c-2", "title": "Oblig 2", "course": "IN1000", "due": null, "status": "todo"}]
            """;

    @MockitoBean CollectorClient collector;
    @MockitoBean MailService mail;
    @Autowired AuthService auth;
    @Autowired PlatformService platforms;
    @Autowired HomeworkSyncService sync;
    @Autowired AssignmentRepository assignments;
    @Autowired JdbcTemplate jdbc;
    @PersistenceContext EntityManager em;

    @BeforeEach
    void needsV21() {
        Boolean exists = jdbc.queryForObject("SELECT to_regproc('sync_assignments') IS NOT NULL", Boolean.class);
        assumeTrue(Boolean.TRUE.equals(exists), "нет функции sync_assignments (миграция V21)");
    }

    @Test
    void connectThenSyncDoesNotDuplicateOrResetSubmitted() throws Exception {
        Long userId = TestUsers.register(auth, mail, "sync-" + System.nanoTime() + "@example.com",
                "password123", "Sync Elev").user().id();
        when(collector.collect(anyMap())).thenReturn(new CollectResult(JSON.readTree(ITEMS),
                Map.of("canvas", new Source(true, 2, null, false)), Map.of()));

        // так же, как шлёт фронт: POST /platforms/canvas {"base_url": …, "token": …}
        platforms.connect(userId, PlatformEnum.CANVAS,
                JSON.readTree("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"very-secret-canvas-token\"}"));

        assertThat(assignments.findForUser(userId)).hasSize(2);
        String stored = jdbc.queryForObject(
                "SELECT encode(credentials_enc, 'escape') FROM platform_connections WHERE user_id = ?", String.class, userId);
        assertThat(stored).doesNotContain("very-secret-canvas-token").doesNotContain("base_url");

        // ученик отметил «сдано» на сайте, платформа всё ещё показывает todo
        Long first = assignments.findForUser(userId).get(0).getId();
        jdbc.update("UPDATE assignments SET status = 'submitted' WHERE id = ?", first);

        SyncResponseDTO res = sync.syncNow(userId);
        em.clear();   // UPDATE и sync_assignments шли мимо Hibernate

        assertThat(res.added()).isZero();
        assertThat(res.updated()).isEqualTo(2);
        assertThat(res.sources()).singleElement().satisfies(s -> {
            assertThat(s.platform()).isEqualTo(PlatformEnum.CANVAS);
            assertThat(s.ok()).isTrue();
        });
        assertThat(assignments.findForUser(userId)).hasSize(2);
        assertThat(assignments.findById(first)).get().extracting(a -> a.getStatus())
                .isEqualTo(AssignmentStatusEnum.SUBMITTED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_events WHERE user_id = ? AND type = 'homework_synced'",
                Integer.class, userId)).isEqualTo(2);
    }
}
