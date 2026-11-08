package com.lekseradar.service.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.exception.ApiException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Обмен кода и обновление токена против заглушки oauth2.googleapis.com/token. */
class GoogleOAuthClientTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BACK = "http://localhost:5173/settings/platforms";
    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final String ALL_SCOPES = "https://www.googleapis.com/auth/classroom.courses.readonly "
            + "https://www.googleapis.com/auth/classroom.student-submissions.me.readonly";

    private HttpServer server;
    private final AtomicReference<String> lastForm = new AtomicReference<>();
    private final AtomicReference<Integer> status = new AtomicReference<>(200);
    private final AtomicReference<String> reply = new AtomicReference<>();
    private final AtomicReference<String> revoked = new AtomicReference<>();
    private final AtomicReference<Integer> revokeStatus = new AtomicReference<>(200);
    private GoogleOAuthClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", ex -> {
            lastForm.set(URLDecoder.decode(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            byte[] out = reply.get().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status.get(), out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.createContext("/revoke", ex -> {
            revoked.set(URLDecoder.decode(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            ex.sendResponseHeaders(revokeStatus.get(), -1);
            ex.close();
        });
        server.start();
        client = new GoogleOAuthClient("id-123.apps.googleusercontent.com", "GOCSPX-secret", "https://accounts.google.com/o/oauth2/v2/auth",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/token", Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void revokeSendsRefreshTokenToGoogle() throws Exception {
        client.revoke(JSON.readTree("{\"access_token\": \"ya29.a\", \"refresh_token\": \"1//r\"}"));
        assertEquals("token=1//r", revoked.get());
    }

    @Test
    void revokeFailureDoesNotThrow() throws Exception {
        revokeStatus.set(400);   // invalid_token: уже отозван
        assertDoesNotThrow(() -> client.revoke(JSON.readTree("{\"access_token\": \"ya29.a\"}")));
        assertEquals("token=ya29.a", revoked.get());
    }

    @Test
    void authorizationUrlAsksForOfflineReadOnlyAccess() {
        String url = URLDecoder.decode(client.authorizationUrl("st4te", BACK), StandardCharsets.UTF_8);
        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
        assertTrue(url.contains("client_id=id-123.apps.googleusercontent.com"));
        assertTrue(url.contains("redirect_uri=" + BACK + "&"));
        assertTrue(url.contains("classroom.courses.readonly") && url.contains("classroom.coursework.me.readonly"));
        assertTrue(url.contains("access_type=offline") && url.contains("prompt=consent") && url.contains("state=st4te"));
        assertFalse(url.contains("GOCSPX"), "секрет не должен попадать в ссылку");
    }

    @Test
    void exchangeGivesCollectorCredentials() {
        reply.set("{\"access_token\": \"ya29.a\", \"expires_in\": 3599, \"refresh_token\": \"1//r\", \"scope\": \"" + ALL_SCOPES + "\"}");
        JsonNode creds = client.exchange("4/code", BACK);
        assertEquals("ya29.a", creds.path("access_token").asText());
        assertEquals("1//r", creds.path("refresh_token").asText());
        assertEquals(NOW.getEpochSecond() + 3599, creds.path("expires_at").asLong());
        assertTrue(lastForm.get().contains("grant_type=authorization_code") && lastForm.get().contains("code=4/code"));
        assertTrue(lastForm.get().contains("redirect_uri=" + BACK));
    }

    @Test
    void uncheckedScopesAre422() {
        reply.set("{\"access_token\": \"ya29.a\", \"expires_in\": 3599, \"scope\": \"https://www.googleapis.com/auth/classroom.courses.readonly\"}");
        ApiException e = assertThrows(ApiException.class, () -> client.exchange("4/code", BACK));
        assertEquals(422, e.getStatus().value());
    }

    @Test
    void oldCodeIs422AndWrongRedirectIs503() {
        status.set(400);
        reply.set("{\"error\": \"invalid_grant\", \"error_description\": \"Bad Request\"}");
        assertEquals(422, assertThrows(ApiException.class, () -> client.exchange("4/old", BACK)).getStatus().value());
        reply.set("{\"error\": \"redirect_uri_mismatch\"}");
        ApiException e = assertThrows(ApiException.class, () -> client.exchange("4/x", BACK));
        assertEquals(503, e.getStatus().value());
        assertTrue(e.getMessage().contains("redirect URIs"));
    }

    @Test
    void freshTokenIsKeptExpiringIsRefreshed() throws Exception {
        JsonNode live = JSON.readTree("{\"access_token\": \"a\", \"refresh_token\": \"1//r\", \"expires_at\": " + (NOW.getEpochSecond() + 3000) + "}");
        assertSame(live, client.fresh(live));

        JsonNode old = JSON.readTree("{\"access_token\": \"a\", \"refresh_token\": \"1//r\", \"expires_at\": " + (NOW.getEpochSecond() + 30) + "}");
        reply.set("{\"access_token\": \"ya29.new\", \"expires_in\": 3599}");
        JsonNode fresh = client.fresh(old);
        assertEquals("ya29.new", fresh.path("access_token").asText());
        assertEquals("1//r", fresh.path("refresh_token").asText(), "refresh_token сохраняется");
        assertTrue(lastForm.get().contains("grant_type=refresh_token") && lastForm.get().contains("refresh_token=1//r"));
    }

    @Test
    void revokedRefreshTokenAsksToReconnect() throws Exception {
        status.set(400);
        reply.set("{\"error\": \"invalid_grant\", \"error_description\": \"Token has been expired or revoked.\"}");
        JsonNode old = JSON.readTree("{\"access_token\": \"a\", \"refresh_token\": \"1//r\", \"expires_at\": 1}");
        ApiException e = assertThrows(ApiException.class, () -> client.fresh(old));
        assertEquals(422, e.getStatus().value());
        assertEquals(GoogleOAuthClient.RECONNECT, e.getMessage());
    }

    @Test
    void notConfiguredIs503() {
        GoogleOAuthClient empty = new GoogleOAuthClient("", "", "https://a", "https://t", Clock.systemUTC());
        assertFalse(empty.configured());
        assertEquals(503, assertThrows(ApiException.class, () -> empty.authorizationUrl("s", BACK)).getStatus().value());
    }
}
