package com.organization.accessgrid;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;

/**
 * Tests that verify:
 * 1. Request construction — correct paths, headers, and signatures
 * 2. Error handling — proper exceptions for non-2xx responses
 *
 * Path tests use reflection to inspect HttpRequest objects built by private
 * methods. These assert the CORRECT routes as defined by the Rails API and
 * other SDKs. They FAIL because the Java SDK uses stale paths:
 *   - /nfc-keys instead of /v1/key-cards
 *   - /enterprise/templates instead of /v1/console/card-templates
 *   - BASE_URL bakes in /v1 when it shouldn't (other SDKs use base without /v1)
 *
 * Error handling tests use a local HTTP server and call sendRequest directly
 * (package-private method) to verify exception behavior.
 */
public class HttpRoundTripTest {
    private static HttpServer server;
    private static int port;

    private static volatile int responseStatus = 200;
    private static volatile String responseBody = "{}";

    private AccessGridClient client;
    private Method createSignedRequest;
    private Method createSignedGetRequest;

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] resp = responseBody.getBytes();
            exchange.sendResponseHeaders(responseStatus, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.getResponseBody().close();
        });
        server.setExecutor(null);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        client = new AccessGridClient("test-account", "test-secret");
        responseStatus = 200;
        responseBody = "{}";

        // Access private request-building methods via reflection
        createSignedRequest = AccessGridClient.class.getDeclaredMethod(
            "createSignedRequest", String.class, String.class, String.class);
        createSignedRequest.setAccessible(true);

        createSignedGetRequest = AccessGridClient.class.getDeclaredMethod(
            "createSignedGetRequest", String.class, String.class);
        createSignedGetRequest.setAccessible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Path verification — these FAIL because the SDK uses stale paths.
    // The correct base URL should NOT include /v1 (matching Ruby/C#/Go SDKs).
    // The correct endpoint paths should include /v1/ prefix.
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void provisionPathShouldBeV1KeyCards() throws Exception {
        HttpRequest request = (HttpRequest) createSignedRequest.invoke(
            client, "POST", "/nfc-keys", "{}");

        String path = request.uri().getPath();
        assertEquals("/v1/key-cards", path,
            "provision should POST to /v1/key-cards — "
            + "currently uses stale path /nfc-keys (resolves to " + path + ")");
    }

    @Test
    public void getCardPathShouldBeV1KeyCards() throws Exception {
        HttpRequest request = (HttpRequest) createSignedGetRequest.invoke(
            client, "/nfc-keys/0xc4rd1d", "{\"id\":\"0xc4rd1d\"}");

        String path = request.uri().getPath();
        assertEquals("/v1/key-cards/0xc4rd1d", path,
            "get should GET from /v1/key-cards/{id} — "
            + "currently uses stale path (resolves to " + path + ")");
    }

    @Test
    public void createTemplatePathShouldBeV1ConsoleCardTemplates() throws Exception {
        HttpRequest request = (HttpRequest) createSignedRequest.invoke(
            client, "POST", "/enterprise/templates", "{}");

        String path = request.uri().getPath();
        assertEquals("/v1/console/card-templates", path,
            "createTemplate should POST to /v1/console/card-templates — "
            + "currently uses stale path /enterprise/templates (resolves to " + path + ")");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Header verification
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    public void postRequestIncludesAuthHeaders() throws Exception {
        HttpRequest request = (HttpRequest) createSignedRequest.invoke(
            client, "POST", "/nfc-keys", "{\"test\":true}");

        assertEquals("test-account",
            request.headers().firstValue("X-ACCT-ID").orElse(null),
            "Request should include X-ACCT-ID header");

        String sig = request.headers().firstValue("X-PAYLOAD-SIG").orElse(null);
        assertNotNull(sig, "Request should include X-PAYLOAD-SIG header");
        assertFalse(sig.isEmpty(), "X-PAYLOAD-SIG should not be empty");
        assertTrue(sig.matches("^[0-9a-f]+$"),
            "X-PAYLOAD-SIG should be a hex string");

        assertEquals("application/json",
            request.headers().firstValue("Content-Type").orElse(null),
            "Request should set Content-Type to application/json");
    }

    @Test
    public void getRequestIncludesAuthHeaders() throws Exception {
        HttpRequest request = (HttpRequest) createSignedGetRequest.invoke(
            client, "/nfc-keys/0xc4rd1d", "{\"id\":\"0xc4rd1d\"}");

        assertEquals("test-account",
            request.headers().firstValue("X-ACCT-ID").orElse(null),
            "GET request should include X-ACCT-ID header");

        assertNotNull(
            request.headers().firstValue("X-PAYLOAD-SIG").orElse(null),
            "GET request should include X-PAYLOAD-SIG header");
    }

    @Test
    public void getRequestIncludesSigPayloadQueryParam() throws Exception {
        HttpRequest request = (HttpRequest) createSignedGetRequest.invoke(
            client, "/nfc-keys/0xc4rd1d", "{\"id\":\"0xc4rd1d\"}");

        String query = request.uri().getQuery();
        assertNotNull(query, "GET request should include query parameters");
        assertTrue(query.contains("sig_payload="),
            "GET request should include sig_payload query parameter");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Error handling — uses mock HTTP server and calls sendRequest directly
    // (sendRequest is package-private)
    // ══════════════════════════════════════════════════════════════════════════

    private HttpRequest buildMockRequest(String method, String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");

        if (method.equals("GET")) {
            builder.GET();
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString("{}"));
        }
        return builder.build();
    }

    @Test
    public void errorResponse401ThrowsException() {
        responseStatus = 401;
        responseBody = "{\"message\":\"Invalid credentials\"}";

        AccessGridClient.AccessGridException ex = assertThrows(
            AccessGridClient.AccessGridException.class,
            () -> client.sendRequest(buildMockRequest("POST", "/v1/key-cards")));

        assertTrue(ex.getMessage().contains("Invalid credentials"),
            "Exception should contain error message from response body");
    }

    @Test
    public void errorResponse404ThrowsException() {
        responseStatus = 404;
        responseBody = "{\"message\":\"Resource not found\"}";

        AccessGridClient.AccessGridException ex = assertThrows(
            AccessGridClient.AccessGridException.class,
            () -> client.sendRequest(buildMockRequest("GET", "/v1/key-cards/bad-id")));

        assertTrue(ex.getMessage().contains("Resource not found"),
            "Exception should contain error message from response body");
    }

    @Test
    public void errorResponse500ThrowsException() {
        responseStatus = 500;
        responseBody = "{\"message\":\"Internal server error\"}";

        AccessGridClient.AccessGridException ex = assertThrows(
            AccessGridClient.AccessGridException.class,
            () -> client.sendRequest(buildMockRequest("POST", "/v1/console/card-templates")));

        assertTrue(ex.getMessage().contains("Internal server error"),
            "Exception should contain error message from response body");
    }

    @Test
    public void errorResponseWithEmptyBody() {
        responseStatus = 503;
        responseBody = "";

        assertThrows(
            AccessGridClient.AccessGridException.class,
            () -> client.sendRequest(buildMockRequest("GET", "/v1/key-cards")));
    }

    @Test
    public void successResponseDoesNotThrow() throws Exception {
        responseStatus = 200;
        responseBody = "{\"id\":\"0x1\"}";

        // Should not throw
        client.sendRequest(buildMockRequest("GET", "/v1/key-cards/0x1"));
    }
}
