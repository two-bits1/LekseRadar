package com.lekseradar.service.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.exception.ApiException;
import com.lekseradar.model.enums.PlatformEnum;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/** Клиент против заглушки collector на JDK HttpServer. */
class CollectorClientTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final Map<String, String> requests = new ConcurrentHashMap<>();
    private final Map<String, String> keys = new ConcurrentHashMap<>();
    private CollectorClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        route("/collect", 200, """
                {"generated_at": "2026-11-07T12:00:00Z",
                 "assignments": [{"source": "canvas", "id": "1", "title": "Oblig 1", "course": "IN1000", "status": "todo"}],
                 "sources": {"canvas": {"ok": true, "count": 1, "error": null, "needs_reconnect": false},
                             "inschool": {"ok": false, "count": 0, "error": "Сессия истекла", "needs_reconnect": true}},
                 "updated_credentials": {"canvas": {"base_url": "https://uio.instructure.com", "token": "new"}}}
                """);
        route("/login/teams/poll", 200, "{\"pending\": true}");
        route("/login/itslearning/exchange", 400, "{\"detail\": \"Код входа устарел\", \"needs_reconnect\": false}");
        route("/login/teams/start", 401, "{\"detail\": \"bad key\"}");
        route("/login/itslearning/url", 502, "{\"detail\": \"itslearning svarer ikke\"}");
        server.start();
        client = new CollectorClient("http://127.0.0.1:" + server.getAddress().getPort(), "k3y", Duration.ofSeconds(5));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void route(String path, int status, String body) {
        server.createContext(path, ex -> {
            requests.put(path, new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            keys.put(path, String.valueOf(ex.getRequestHeaders().getFirst("X-Internal-Key")));
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
    }

    @Test
    void collectParsesSnakeCase() throws Exception {
        var creds = JSON.readTree("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"t\"}");
        CollectorClient.CollectResult r = client.collect(Map.of(PlatformEnum.CANVAS, creds));

        assertEquals("k3y", keys.get("/collect"));
        assertEquals(creds, JSON.readTree(requests.get("/collect")).path("canvas"));
        assertEquals(1, r.assignments().size());
        assertEquals(new CollectorClient.Source(true, 1, null, false), r.source(PlatformEnum.CANVAS));
        assertEquals(new CollectorClient.Source(false, 0, "Сессия истекла", true), r.source(PlatformEnum.INSCHOOL));
        assertEquals("new", r.updatedCredentials().get("canvas").path("token").asText());
    }

    @Test
    void checkTellsOkWrongKeyAndUnreachable() throws Exception {
        assertEquals(CollectorClient.Status.OK, client.check());

        HttpServer strict = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        strict.createContext("/collect", ex -> {
            byte[] out = "{\"detail\": \"invalid internal key\"}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(401, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        strict.start();
        try {
            var wrongKey = new CollectorClient("http://127.0.0.1:" + strict.getAddress().getPort(), "bad", Duration.ofSeconds(5));
            assertEquals(CollectorClient.Status.WRONG_KEY, wrongKey.check());
        } finally {
            strict.stop(0);
        }
        server.stop(0);
        assertEquals(CollectorClient.Status.UNREACHABLE, client.check());
    }

    @Test
    void teamsPollPendingIsEmpty() throws Exception {
        assertTrue(client.teamsPoll("dev-code").isEmpty());
        assertEquals("dev-code", JSON.readTree(requests.get("/login/teams/poll")).path("device_code").asText());
    }

    @Test
    void sourceErrorBecomes422() {
        ApiException e = assertThrows(ApiException.class,
                () -> client.itslearningExchange("https://x.itslearning.com", "c"));
        assertEquals(422, e.getStatus().value());
        assertEquals("Код входа устарел", e.getMessage());
    }

    @Test
    void platformDownAtLoginIs502WithText() {
        ApiException e = assertThrows(ApiException.class, () -> client.itslearningUrl("https://sms.itslearning.com"));
        assertEquals(502, e.getStatus().value());
        assertEquals("itslearning svarer ikke", e.getMessage());
    }

    @Test
    void wrongKeyOrDownBecomes503NeverUnauthorized() {
        assertEquals(503, assertThrows(ApiException.class, client::teamsStart).getStatus().value());
        server.stop(0);
        assertEquals(503, assertThrows(ApiException.class, () -> client.collect(Map.of())).getStatus().value());
    }
}
