package com.lekseradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.config.AppConfig;
import com.lekseradar.exception.ApiException;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.model.PlatformConnectionEntity;
import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.repository.PlatformConnectionRepository;
import com.lekseradar.security.CredentialsCipher;
import com.lekseradar.security.RateLimiter;
import com.lekseradar.service.collector.CollectorClient;
import com.lekseradar.service.collector.CollectorClient.CollectResult;
import com.lekseradar.service.collector.CollectorClient.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/** POST /platforms/{platform}: тело — как его собирает фронт (toCredentials в Platforms.tsx), snake_case. */
class PlatformServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long USER = 7L;

    private PlatformConnectionRepository connections;
    private CollectorClient collector;
    private HomeworkSyncService sync;
    private CredentialsCipher cipher;
    private PlatformService service;

    @BeforeEach
    void setUp() {
        connections = mock(PlatformConnectionRepository.class);
        collector = mock(CollectorClient.class);
        sync = mock(HomeworkSyncService.class);
        cipher = new CredentialsCipher(Base64.getEncoder().encodeToString(new byte[32]), JSON);
        when(connections.findByUserIdAndPlatform(anyLong(), any())).thenReturn(Optional.empty());
        AppConfig cfg = new AppConfig(null, null, null, null, null, new AppConfig.Platforms(List.of("canvas.uio.no")), null);
        service = new PlatformService(connections, collector, cipher, sync, new RateLimiter(), JSON,
                mock(PlatformTransactionManager.class), cfg);
    }

    private static JsonNode body(String json) throws Exception {
        return JSON.readTree(json);
    }

    private static JsonNode canvas(String baseUrl, String token) {
        return JSON.createObjectNode().put("base_url", baseUrl).put("token", token);
    }

    private static CollectResult ok(PlatformEnum p) throws Exception {
        return new CollectResult(JSON.readTree("[{\"source\": \"" + p.value() + "\", \"id\": \"1\"}]"),
                Map.of(p.value(), new Source(true, 1, null, false)), Map.of());
    }

    /** Что сохранено в базе (расшифровано). */
    private JsonNode saved() {
        ArgumentCaptor<PlatformConnectionEntity> c = ArgumentCaptor.forClass(PlatformConnectionEntity.class);
        verify(connections).save(c.capture());
        return cipher.decrypt(c.getValue().getCredentialsEnc());
    }

    /** Что ушло в collector. */
    @SuppressWarnings("unchecked")
    private JsonNode sentToCollector(PlatformEnum p) {
        ArgumentCaptor<Map<PlatformEnum, JsonNode>> c = ArgumentCaptor.forClass(Map.class);
        verify(collector).collect(c.capture());
        return c.getValue().get(p);
    }

    @Test
    void canvasFromFrontendIsSavedEncrypted() throws Exception {
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.CANVAS));

        var res = service.connect(USER, PlatformEnum.CANVAS, canvas("https://UiO.instructure.com/courses?x=1", " secret-token "));

        assertTrue(res.connected());
        ArgumentCaptor<PlatformConnectionEntity> c = ArgumentCaptor.forClass(PlatformConnectionEntity.class);
        verify(connections).save(c.capture());
        byte[] enc = c.getValue().getCredentialsEnc();
        assertFalse(new String(enc, StandardCharsets.ISO_8859_1).contains("secret-token"));
        assertEquals(body("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"secret-token\"}"), cipher.decrypt(enc));
        verify(sync).store(eq(USER), any());
    }

    @Test
    void onlyKnownFieldsGoToCollector() throws Exception {
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.CANVAS));
        service.connect(USER, PlatformEnum.CANVAS,
                body("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"t\", \"evil\": \"x\"}"));
        assertEquals(body("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"t\"}"), sentToCollector(PlatformEnum.CANVAS));
    }

    @Test
    void extraCanvasHostFromConfigIsAllowed() throws Exception {
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.CANVAS));
        assertTrue(service.connect(USER, PlatformEnum.CANVAS, canvas("https://canvas.uio.no", "t")).connected());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://uio.instructure.com", "https://evil.com", "https://instructure.com.evil.com",
            "https://uio.instructure.com:8443", "https://user@uio.instructure.com", "https://localhost",
            "https://169.254.169.254", "file:///etc/passwd", "not a url", "https://canvas.uio.no.evil.com"})
    void badAddressIs400AndNothingIsCalled(String url) {
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.CANVAS, canvas(url, "t")));
        verifyNoInteractions(collector);
        verify(connections, never()).save(any());
    }

    @Test
    void badBodiesAre400() throws Exception {
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.CANVAS, body("[]")));
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.CANVAS, body("{\"base_url\": \"https://uio.instructure.com\"}")));
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.CANVAS, body("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"  \"}")));
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.CANVAS, body("{\"base_url\": 5, \"token\": \"t\"}")));
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.TEAMS, body("{\"access_token\": \"\", \"refresh_token\": \"\"}")));
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.ITSLEARNING, body("{\"base_url\": \"https://sms.itslearning.com\"}")));
        verifyNoInteractions(collector);
    }

    @Test
    void inschoolFromFrontendDropsEmptyCookies() throws Exception {
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.INSCHOOL));
        // так шлёт фронт, если поле Authorization не заполнено
        service.connect(USER, PlatformEnum.INSCHOOL,
                body("{\"host\": \"ohg-vgs.inschool.visma.no\", \"cookies\": {\"JSESSIONID\": \"abc\", \"Authorization\": \"\"}}"));
        assertEquals(body("{\"host\": \"ohg-vgs.inschool.visma.no\", \"cookies\": {\"JSESSIONID\": \"abc\"}}"), saved());
    }

    @Test
    void inschoolHostAsUrlAndBadInput() throws Exception {
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.INSCHOOL));
        service.connect(USER, PlatformEnum.INSCHOOL,
                body("{\"host\": \"https://ohg-vgs.inschool.visma.no/\", \"cookies\": {\"JSESSIONID\": \"abc\"}}"));
        assertEquals("ohg-vgs.inschool.visma.no", saved().path("host").asText());

        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.INSCHOOL,
                body("{\"host\": \"evil.no\", \"cookies\": {\"a\": \"b\"}}")));
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.INSCHOOL,
                body("{\"host\": \"x.inschool.visma.no\", \"cookies\": {\"JSESSIONID\": \"\"}}")));
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.INSCHOOL,
                body("{\"host\": \"x.inschool.visma.no\"}")));
    }

    @Test
    void teamsTokensFromFrontend() throws Exception {
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.TEAMS));
        service.connect(USER, PlatformEnum.TEAMS, body("{\"access_token\": \"\", \"refresh_token\": \"r1\", \"expires_at\": 1762520000}"));
        assertEquals(body("{\"refresh_token\": \"r1\", \"expires_at\": 1762520000}"), saved());
        assertThrows(BadRequestException.class, () -> service.connect(USER, PlatformEnum.TEAMS,
                body("{\"refresh_token\": \"r\", \"client_id\": \"not-a-guid\"}")));
    }

    @Test
    void itslearningWithRefreshTokenNeedsNoExchange() throws Exception {
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.ITSLEARNING));
        service.connect(USER, PlatformEnum.ITSLEARNING, body("{\"base_url\": \"https://sms.itslearning.com/\", \"refresh_token\": \"r\"}"));
        assertEquals(body("{\"base_url\": \"https://sms.itslearning.com\", \"refresh_token\": \"r\"}"), saved());
        verify(collector, never()).itslearningExchange(any(), any());
    }

    @Test
    void itslearningCodeIsExchangedByCollector() throws Exception {
        JsonNode tokens = body("{\"base_url\": \"https://sms.itslearning.com\", \"access_token\": \"a\", \"refresh_token\": \"r\", \"expires_at\": 1}");
        when(collector.itslearningExchange("https://sms.itslearning.com", "itsl-itslearning://login?code=XYZ")).thenReturn(tokens);
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.ITSLEARNING));

        service.connect(USER, PlatformEnum.ITSLEARNING,
                body("{\"base_url\": \"https://sms.itslearning.com\", \"code_or_url\": \"itsl-itslearning://login?code=XYZ\"}"));

        assertEquals(tokens, saved());
    }

    @Test
    void rejectedTokenIs422AndNothingSaved() {
        when(collector.collect(anyMap())).thenReturn(new CollectResult(JSON.createArrayNode(),
                Map.of("canvas", new Source(false, 0, "Canvas: неверный токен", true)), Map.of()));

        ApiException e = assertThrows(ApiException.class,
                () -> service.connect(USER, PlatformEnum.CANVAS, canvas("https://uio.instructure.com", "bad")));
        assertEquals(422, e.getStatus().value());
        assertEquals("Canvas: неверный токен", e.getMessage());
        verify(connections, never()).save(any());
        verifyNoInteractions(sync);
    }

    @Test
    void reconnectUpdatesSameRowWithUpdatedCredentials() throws Exception {
        PlatformConnectionEntity existing = new PlatformConnectionEntity(USER, PlatformEnum.CANVAS, new byte[]{1});
        existing.markSynced(false, "старая ошибка", true);
        when(connections.findByUserIdAndPlatform(USER, PlatformEnum.CANVAS)).thenReturn(Optional.of(existing));
        JsonNode rotated = body("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"rotated\"}");
        when(collector.collect(anyMap())).thenReturn(new CollectResult(JSON.createArrayNode(),
                Map.of("canvas", new Source(true, 0, null, false)), Map.of("canvas", rotated)));

        service.connect(USER, PlatformEnum.CANVAS, canvas("https://uio.instructure.com", "t"));

        verify(connections).save(existing);
        assertFalse(existing.isNeedsReconnect());
        assertNull(existing.getLastError());
        assertEquals(rotated, cipher.decrypt(existing.getCredentialsEnc()));
    }

    @Test
    void teamsPollPendingThenConnected() throws Exception {
        when(collector.teamsPoll("dc")).thenReturn(Optional.empty());
        assertEquals("pending", service.teamsPoll(USER, "dc").status());
        verify(collector, never()).collect(anyMap());

        when(collector.teamsPoll("dc")).thenReturn(Optional.of(body("{\"access_token\": \"a\", \"refresh_token\": \"r\"}")));
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.TEAMS));
        var res = service.teamsPoll(USER, "dc");
        assertEquals("connected", res.status());
        assertEquals(PlatformEnum.TEAMS, res.platform().platform());
    }

    @Test
    void connectLimitIs10Per10Minutes() throws Exception {
        when(collector.collect(anyMap())).thenReturn(ok(PlatformEnum.CANVAS));
        for (int i = 0; i < PlatformService.CONNECT_LIMIT; i++) {
            service.connect(USER, PlatformEnum.CANVAS, canvas("https://uio.instructure.com", "t"));
        }
        TooManyRequestsException e = assertThrows(TooManyRequestsException.class,
                () -> service.connect(USER, PlatformEnum.CANVAS, canvas("https://uio.instructure.com", "t")));
        assertEquals(600L, e.getRetryAfterSeconds());
    }

    @Test
    void listAlwaysHasFourPlatforms() {
        when(connections.findByUserIdOrderByPlatform(USER))
                .thenReturn(List.of(new PlatformConnectionEntity(USER, PlatformEnum.TEAMS, new byte[]{1})));
        var list = service.list(USER);
        assertEquals(4, list.size());
        assertEquals(1, list.stream().filter(p -> p.connected()).count());
        assertTrue(list.stream().filter(p -> p.platform() == PlatformEnum.TEAMS).findFirst().orElseThrow().connected());
    }
}
