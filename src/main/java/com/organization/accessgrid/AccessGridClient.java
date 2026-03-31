package com.organization.accessgrid;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Client for interacting with the Access Grid API.
 */
public class AccessGridClient {
    private static final String DEFAULT_BASE_URL = "https://api.accessgrid.com/v1";
    private static final String VERSION = "1.2.0";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String accountId;
    private final String apiSecret;
    private final HttpSender httpSender;
    final ObjectMapper objectMapper;
    private final String baseUrl;

    /**
     * Constructor for AccessGridClient.
     *
     * @param accountId The account identifier
     * @param apiSecret The API secret key
     */
    public AccessGridClient(String accountId, String apiSecret) {
        this(accountId, apiSecret, (HttpSender) null, null);
    }

    /**
     * Constructor for AccessGridClient with optional HttpSender and base URL.
     *
     * @param accountId  The account identifier
     * @param apiSecret  The API secret key
     * @param httpSender Optional HttpSender for sending requests (creates default if null)
     * @param baseUrl    Optional base URL (defaults to https://api.accessgrid.com/v1)
     */
    public AccessGridClient(String accountId, String apiSecret, HttpSender httpSender, String baseUrl) {
        if (accountId == null || accountId.isEmpty())
            throw new AccessGridException("Account ID is required");
        if (apiSecret == null || apiSecret.isEmpty())
            throw new AccessGridException("API secret key is required");

        this.accountId = accountId;
        this.apiSecret = apiSecret;
        this.baseUrl = (baseUrl != null) ? baseUrl : DEFAULT_BASE_URL;

        if (httpSender != null) {
            this.httpSender = httpSender;
        } else {
            HttpClient defaultClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
            this.httpSender = (request) -> defaultClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Get the account ID.
     */
    public String getAccountId() {
        return this.accountId;
    }

    /**
     * Access Cards API operations.
     */
    public AccessCardsApi accessCards() {
        return new AccessCardsApi(this);
    }

    /**
     * Console Management API operations.
     */
    public ConsoleApi console() {
        return new ConsoleApi(this);
    }

    /**
     * API for Access Card operations.
     */
    public static class AccessCardsApi {
        private final AccessGridClient client;

        AccessCardsApi(AccessGridClient client) {
            this.client = client;
        }

        /**
         * Provision a new access card.
         */
        public Models.Card provision(Models.ProvisionCardRequest request) {
            String payload = client.serialize(request);
            return client.post("/key-cards", payload, Models.Card.class);
        }

        /**
         * Get details about a specific access card.
         */
        public Models.Card get(String cardId) {
            return client.get("/key-cards/" + cardId, cardId, Models.Card.class);
        }
    }

    /**
     * API for Console Management operations.
     */
    public static class ConsoleApi {
        private final AccessGridClient client;

        ConsoleApi(AccessGridClient client) {
            this.client = client;
        }

        /**
         * Create a new card template.
         */
        public Models.Template createTemplate(Models.CreateTemplateRequest request) {
            String payload = client.serialize(request);
            return client.post("/console/card-templates", payload, Models.Template.class);
        }
    }

    // --- Internal HTTP methods ---

    <T> T post(String path, String payload, Class<T> responseType) {
        try {
            String signature = generateSignature(payload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-ACCT-ID", accountId)
                .header("X-PAYLOAD-SIG", signature)
                .header("User-Agent", "accessgrid.java/" + VERSION)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = sendRequest(request);
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException | InterruptedException e) {
            throw new AccessGridException("API request failed", e);
        }
    }

    <T> T put(String path, String payload, Class<T> responseType) {
        try {
            String signature = generateSignature(payload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-ACCT-ID", accountId)
                .header("X-PAYLOAD-SIG", signature)
                .header("User-Agent", "accessgrid.java/" + VERSION)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = sendRequest(request);
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException | InterruptedException e) {
            throw new AccessGridException("API request failed", e);
        }
    }

    <T> T patch(String path, String payload, Class<T> responseType) {
        try {
            String signature = generateSignature(payload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-ACCT-ID", accountId)
                .header("X-PAYLOAD-SIG", signature)
                .header("User-Agent", "accessgrid.java/" + VERSION)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = sendRequest(request);
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException | InterruptedException e) {
            throw new AccessGridException("API request failed", e);
        }
    }

    <T> T get(String path, String resourceId, Class<T> responseType) {
        try {
            String idPayload = "{\"id\": \"" + resourceId + "\"}";
            String signature = generateSignature(idPayload);
            String encodedPayload = java.net.URLEncoder.encode(idPayload, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path + "?sig_payload=" + encodedPayload))
                .header("X-ACCT-ID", accountId)
                .header("X-PAYLOAD-SIG", signature)
                .header("User-Agent", "accessgrid.java/" + VERSION)
                .header("Content-Type", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = sendRequest(request);
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException | InterruptedException e) {
            throw new AccessGridException("API request failed", e);
        }
    }

    <T> T getWithParams(String path, String queryString, Class<T> responseType) {
        try {
            String signature = generateSignature("{}");
            String separator = queryString.isEmpty() ? "?" : queryString + "&";
            String uri = baseUrl + path + "?" + queryString + (queryString.isEmpty() ? "" : "&") + "sig_payload=" + java.net.URLEncoder.encode("{}", StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path + (queryString.isEmpty() ? "" : "?" + queryString)))
                .header("X-ACCT-ID", accountId)
                .header("X-PAYLOAD-SIG", signature)
                .header("User-Agent", "accessgrid.java/" + VERSION)
                .header("Content-Type", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = sendRequest(request);
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException | InterruptedException e) {
            throw new AccessGridException("API request failed", e);
        }
    }

    void postEmpty(String path, String resourceId) {
        try {
            String idPayload = "{\"id\": \"" + resourceId + "\"}";
            String signature = generateSignature(idPayload);
            String encodedPayload = java.net.URLEncoder.encode(idPayload, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path + "?sig_payload=" + encodedPayload))
                .header("X-ACCT-ID", accountId)
                .header("X-PAYLOAD-SIG", signature)
                .header("User-Agent", "accessgrid.java/" + VERSION)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            sendRequest(request);
        } catch (IOException | InterruptedException e) {
            throw new AccessGridException("API request failed", e);
        }
    }

    void delete(String path) {
        try {
            String signature = generateSignature("{}");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-ACCT-ID", accountId)
                .header("X-PAYLOAD-SIG", signature)
                .header("User-Agent", "accessgrid.java/" + VERSION)
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

            sendRequest(request);
        } catch (IOException | InterruptedException e) {
            throw new AccessGridException("API request failed", e);
        }
    }

    String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            throw new AccessGridException("Failed to serialize request", e);
        }
    }

    HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpSender.send(request);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AccessGridException("API request failed: " + response.body());
        }

        return response;
    }

    String generateSignature(String payload) {
        try {
            String encodedPayload = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hashBytes = sha256Hmac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AccessGridException("Failed to generate signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Custom exception for Access Grid API errors.
     */
    public static class AccessGridException extends RuntimeException {
        public AccessGridException(String message) {
            super(message);
        }

        public AccessGridException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
