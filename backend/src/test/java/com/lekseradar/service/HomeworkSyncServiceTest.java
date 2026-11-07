package com.lekseradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.dto.homework.SourceStatusResponseDTO;
import com.lekseradar.dto.homework.SyncResponseDTO;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.model.PlatformConnectionEntity;
import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.repository.AssignmentRepository;
import com.lekseradar.repository.PlatformConnectionRepository;
import com.lekseradar.repository.UserEventRepository;
import com.lekseradar.security.CredentialsCipher;
import com.lekseradar.security.RateLimiter;
import com.lekseradar.service.collector.CollectorClient;
import com.lekseradar.service.collector.CollectorClient.CollectResult;
import com.lekseradar.service.collector.CollectorClient.Source;
import com.lekseradar.service.google.GoogleOAuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HomeworkSyncServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long USER = 7L;

    private PlatformConnectionRepository connections;
    private AssignmentRepository assignments;
    private UserEventRepository events;
    private CollectorClient collector;
    private CredentialsCipher cipher;
    private HomeworkSyncService service;
    private GoogleOAuthClient google;

    @BeforeEach
    void setUp() {
        connections = mock(PlatformConnectionRepository.class);
        assignments = mock(AssignmentRepository.class);
        events = mock(UserEventRepository.class);
        collector = mock(CollectorClient.class);
        cipher = new CredentialsCipher(Base64.getEncoder().encodeToString(new byte[32]), JSON);
        when(assignments.syncAssignments(anyLong(), anyString())).thenReturn(new AssignmentRepository.SyncCounts() {
            public Integer getAdded() { return 2; }
            public Integer getUpdated() { return 1; }
        });
        google = mock(GoogleOAuthClient.class);
        when(google.fresh(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new HomeworkSyncService(connections, assignments, events, collector, cipher, new RateLimiter(), JSON,
                mock(PlatformTransactionManager.class), google);
    }

    private PlatformConnectionEntity connection(long id, PlatformEnum p, String creds) throws Exception {
        PlatformConnectionEntity c = new PlatformConnectionEntity(USER, p, cipher.encrypt(JSON.readTree(creds)));
        ReflectionTestUtils.setField(c, "id", id);
        when(connections.findById(id)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void updatedTokensAreSavedEncryptedAndReconnectIsMarked() throws Exception {
        PlatformConnectionEntity teams = connection(1, PlatformEnum.TEAMS, "{\"access_token\": \"old\", \"refresh_token\": \"r1\"}");
        PlatformConnectionEntity inschool = connection(2, PlatformEnum.INSCHOOL, "{\"host\": \"x.inschool.visma.no\", \"cookies\": {\"a\": \"b\"}}");
        when(connections.findByUserIdOrderByPlatform(USER)).thenReturn(List.of(teams, inschool));
        JsonNode rotated = JSON.readTree("{\"access_token\": \"new\", \"refresh_token\": \"r2\"}");
        JsonNode items = JSON.readTree("[{\"source\": \"teams\", \"id\": \"a1\", \"title\": \"T\", \"status\": \"todo\"}]");
        when(collector.collect(anyMap())).thenReturn(new CollectResult(items,
                Map.of("teams", new Source(true, 1, null, false),
                       "inschool", new Source(false, 0, "Сессия InSchool истекла", true)),
                Map.of("teams", rotated)));

        SyncResponseDTO res = service.sync(USER);

        // collector получил расшифрованные доступы обеих платформ
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<PlatformEnum, JsonNode>> sent = ArgumentCaptor.forClass(Map.class);
        verify(collector).collect(sent.capture());
        assertEquals("old", sent.getValue().get(PlatformEnum.TEAMS).path("access_token").asText());
        assertEquals(2, sent.getValue().size());

        assertEquals(rotated, cipher.decrypt(teams.getCredentialsEnc()));
        assertNotNull(teams.getLastSyncedAt());
        assertTrue(inschool.isNeedsReconnect());
        assertEquals("Сессия InSchool истекла", inschool.getLastError());
        assertNull(inschool.getLastSyncedAt());

        assertEquals(List.of(new SourceStatusResponseDTO(PlatformEnum.TEAMS, true, 1, null, false),
                        new SourceStatusResponseDTO(PlatformEnum.INSCHOOL, false, 0, "Сессия InSchool истекла", true)),
                res.sources());
        assertEquals(2, res.added());
        assertEquals(1, res.updated());
        verify(assignments).syncAssignments(eq(USER), argThat(s -> s.contains("\"a1\"")));
        verify(events).addWithPayload(USER, "homework_synced", "{\"added\":2,\"updated\":1}");
    }

    @Test
    void needsReconnectIsSkipped() throws Exception {
        PlatformConnectionEntity c = connection(1, PlatformEnum.CANVAS, "{\"token\": \"t\"}");
        c.markSynced(false, "Токен Canvas отозван", true);
        when(connections.findByUserIdOrderByPlatform(USER)).thenReturn(List.of(c));

        SyncResponseDTO res = service.sync(USER);

        verifyNoInteractions(collector);
        assertEquals(List.of(new SourceStatusResponseDTO(PlatformEnum.CANVAS, false, 0, "Токен Canvas отозван", true)),
                res.sources());
    }

    @Test
    void undecryptableCredentialsAskToReconnect() throws Exception {
        PlatformConnectionEntity c = new PlatformConnectionEntity(USER, PlatformEnum.CANVAS, new byte[40]);
        when(connections.findByUserIdOrderByPlatform(USER)).thenReturn(List.of(c));

        SyncResponseDTO res = service.sync(USER);

        verifyNoInteractions(collector);
        assertTrue(c.isNeedsReconnect());
        verify(connections).save(c);
        assertTrue(res.sources().get(0).needsReconnect());
    }

    @Test
    void noConnectionsGivesEmptySources() {
        when(connections.findByUserIdOrderByPlatform(USER)).thenReturn(List.of());
        SyncResponseDTO res = service.sync(USER);
        assertTrue(res.sources().isEmpty());
        assertEquals(0, res.count());
        verifyNoInteractions(collector);
    }

    @Test
    void oneSyncPerMinute() {
        when(connections.findByUserIdOrderByPlatform(USER)).thenReturn(List.of());
        service.sync(USER);
        TooManyRequestsException e = assertThrows(TooManyRequestsException.class, () -> service.sync(USER));
        assertEquals(60L, e.getRetryAfterSeconds());
        service.syncNow(USER);   // расписание лимит не трогает
    }

    @Test
    void classroomTokenIsRefreshedBeforeCollectAndSavedEncrypted() throws Exception {
        PlatformConnectionEntity c = connection(5, PlatformEnum.CLASSROOM, "{\"access_token\": \"old\", \"refresh_token\": \"1//r\", \"expires_at\": 1}");
        when(connections.findByUserIdOrderByPlatform(USER)).thenReturn(List.of(c));
        JsonNode fresh = JSON.readTree("{\"access_token\": \"new\", \"refresh_token\": \"1//r\", \"expires_at\": 9999999999}");
        when(google.fresh(any())).thenReturn(fresh);
        when(collector.collect(anyMap())).thenReturn(new CollectResult(JSON.createArrayNode(),
                Map.of("classroom", new Source(true, 0, null, false)), Map.of()));

        service.syncNow(USER);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<PlatformEnum, JsonNode>> sent = ArgumentCaptor.forClass(Map.class);
        verify(collector).collect(sent.capture());
        assertEquals("new", sent.getValue().get(PlatformEnum.CLASSROOM).path("access_token").asText());
        assertEquals(fresh, cipher.decrypt(c.getCredentialsEnc()));
    }

    @Test
    void revokedGoogleAccessAsksToReconnect() throws Exception {
        PlatformConnectionEntity c = connection(5, PlatformEnum.CLASSROOM, "{\"access_token\": \"old\", \"refresh_token\": \"1//r\", \"expires_at\": 1}");
        when(connections.findByUserIdOrderByPlatform(USER)).thenReturn(List.of(c));
        when(google.fresh(any())).thenThrow(new com.lekseradar.exception.ApiException(
                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Войдите в Google Classroom заново"));

        SyncResponseDTO res = service.syncNow(USER);

        verifyNoInteractions(collector);
        assertTrue(c.isNeedsReconnect());
        assertEquals(List.of(new SourceStatusResponseDTO(PlatformEnum.CLASSROOM, false, 0, "Войдите в Google Classroom заново", true)),
                res.sources());
    }
}
