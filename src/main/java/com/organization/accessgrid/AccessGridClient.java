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
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Client for interacting with the Access Grid API.
 */
public class AccessGridClient {
    private static final String BASE_URL = "https://api.accessgrid.com/v1";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String accountId;
    private final String apiSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for AccessGridClient.
     * 
     * @param accountId The account identifier
     * @param apiSecret The API secret key
     */
    public AccessGridClient(String accountId, String apiSecret) {
        this.accountId = accountId;
        this.apiSecret = apiSecret;

        // Configure HTTP client
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();

        // Configure Object Mapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Get the account ID.
     * 
     * @return The account ID
     */
    public String getAccountId() {
        return this.accountId;
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
         * Provision a new access card or unified access pass.
         * Returns a UnifiedAccessPass when issuing to a template pair (contains both Apple and Android cards).
         * Returns a Card when issuing to a single template.
         */
        public Models.Union provision(Models.ProvisionCardRequest request) {
            try {
                String payload = client.objectMapper.writeValueAsString(request);
                HttpRequest httpRequest = client.createSignedRequest("POST", "/nfc-keys", payload);
                HttpResponse<String> response = client.sendRequest(httpRequest);

                // Check if response contains a details array (indicates UnifiedAccessPass)
                JsonNode rootNode = client.objectMapper.readTree(response.body());
                JsonNode detailsNode = rootNode.get("details");

                if (detailsNode != null && detailsNode.isArray()) {
                    return client.objectMapper.readValue(response.body(), Models.UnifiedAccessPass.class);
                }

                return client.objectMapper.readValue(response.body(), Models.Card.class);
            } catch (IOException | InterruptedException e) {
                throw new AccessGridException("Error provisioning card", e);
            }
        }

        /**
         * Get details about a specific access card.
         */
        public Models.Card get(String cardId) {
            try {
                String payload = client.objectMapper.writeValueAsString(java.util.Collections.singletonMap("id", cardId));
                HttpRequest httpRequest = client.createSignedGetRequest("/nfc-keys/" + cardId, payload);
                HttpResponse<String> response = client.sendRequest(httpRequest);

                return client.objectMapper.readValue(response.body(), Models.Card.class);
            } catch (IOException | InterruptedException e) {
                throw new AccessGridException("Error getting card", e);
            }
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
            try {
                String payload = client.objectMapper.writeValueAsString(request);
                HttpRequest httpRequest = client.createSignedRequest("POST", "/enterprise/templates", payload);
                HttpResponse<String> response = client.sendRequest(httpRequest);

                return client.objectMapper.readValue(response.body(), Models.Template.class);
            } catch (IOException | InterruptedException e) {
                throw new AccessGridException("Error creating template", e);
            }
        }
    }

    /**
     * Access Cards API operations.
     * 
     * @return AccessCardsApi instance
     */
    public AccessCardsApi accessCards() {
        return new AccessCardsApi(this);
    }

    /**
     * Console Management API operations.
     * 
     * @return ConsoleApi instance
     */
    public ConsoleApi console() {
        return new ConsoleApi(this);
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

    /**
     * Internal method to send HTTP requests.
     */
    HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AccessGridException("API request failed: " + response.body());
        }
        
        return response;
    }

    /**
     * Create a signed HTTP request.
     *
     * @param method HTTP method
     * @param path API endpoint path
     * @param payload Request body
     * @return Signed HTTP request
     */
    private HttpRequest createSignedRequest(String method, String path, String payload) {
        String signature = generateSignature(payload);

        return HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .header("X-ACCT-ID", this.accountId)
            .header("X-PAYLOAD-SIG", signature)
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(payload))
            .build();
    }

    /**
     * Create a signed HTTP GET request.
     *
     * @param path API endpoint path
     * @param payload Payload for signature
     * @return Signed HTTP GET request
     */
    private HttpRequest createSignedGetRequest(String path, String payload) {
        String signature = generateSignature(payload);
        String encodedPayload = java.net.URLEncoder.encode(payload, StandardCharsets.UTF_8);

        return HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path + "?sig_payload=" + encodedPayload))
            .header("X-ACCT-ID", this.accountId)
            .header("X-PAYLOAD-SIG", signature)
            .header("Content-Type", "application/json")
            .GET()
            .build();
    }

    /**
     * Generate payload signature for API authentication.
     * 
     * @param payload Raw payload to sign
     * @return Base64 encoded signature
     */
    private String generateSignature(String payload) {
        try {
            // Base64 encode the payload first
            String encodedPayload = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

            // Create HMAC-SHA256 hash
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hashBytes = sha256Hmac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AccessGridException("Failed to generate signature", e);
        }
    }

    /**
     * Convert byte array to hexadecimal representation.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}