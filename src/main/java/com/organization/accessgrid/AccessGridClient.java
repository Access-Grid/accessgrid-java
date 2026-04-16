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
    private static final String VERSION = "1.3.0";
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
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
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

        /**
         * Update an existing access card.
         */
        public Models.Card update(Models.UpdateCardRequest request) {
            String payload = client.serialize(request);
            return client.patch("/key-cards/" + request.getCardId(), payload, Models.Card.class);
        }

        /**
         * List access cards with optional filters.
         */
        public java.util.List<Models.Card> list(Models.ListKeysParams params) {
            StringBuilder query = new StringBuilder();
            if (params != null) {
                if (params.getTemplateId() != null)
                    appendParam(query, "template_id", params.getTemplateId());
                if (params.getState() != null)
                    appendParam(query, "state", params.getState());
            }
            Models.ListCardsResponse response = client.getWithParams(
                "/key-cards", query.toString(), Models.ListCardsResponse.class
            );
            return response != null && response.getKeys() != null
                ? response.getKeys()
                : new java.util.ArrayList<>();
        }

        /**
         * List access cards without filters.
         */
        public java.util.List<Models.Card> list() {
            return list(null);
        }

        /**
         * Suspend an access card.
         */
        public void suspend(String cardId) {
            client.postEmpty("/key-cards/" + cardId + "/suspend", cardId);
        }

        /**
         * Resume a suspended access card.
         */
        public void resume(String cardId) {
            client.postEmpty("/key-cards/" + cardId + "/resume", cardId);
        }

        /**
         * Unlink an access card from its device.
         */
        public void unlink(String cardId) {
            client.postEmpty("/key-cards/" + cardId + "/unlink", cardId);
        }

        /**
         * Delete an access card.
         */
        public void delete(String cardId) {
            client.postEmpty("/key-cards/" + cardId + "/delete", cardId);
        }

        private void appendParam(StringBuilder sb, String key, String value) {
            if (sb.length() > 0) sb.append("&");
            sb.append(java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
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

        /**
         * Update an existing card template.
         */
        public Models.Template updateTemplate(Models.UpdateTemplateRequest request) {
            String payload = client.serialize(request);
            return client.put("/console/card-templates/" + request.getCardTemplateId(), payload, Models.Template.class);
        }

        /**
         * Read a card template by ID.
         */
        public Models.Template readTemplate(String templateId) {
            return client.get("/console/card-templates/" + templateId, templateId, Models.Template.class);
        }

        /**
         * Get event logs for a card template.
         */
        public java.util.List<Models.Event> eventLog(String templateId, Models.EventLogFilters filters) {
            StringBuilder query = new StringBuilder();
            if (filters != null) {
                if (filters.getDevice() != null)
                    appendParam(query, "device", filters.getDevice());
                if (filters.getStartDate() != null)
                    appendParam(query, "start_date", filters.getStartDate().toString());
                if (filters.getEndDate() != null)
                    appendParam(query, "end_date", filters.getEndDate().toString());
                if (filters.getEventType() != null)
                    appendParam(query, "event_type", filters.getEventType());
            }
            Models.EventLogResponse response = client.getWithParams(
                "/console/card-templates/" + templateId + "/logs",
                query.toString(),
                Models.EventLogResponse.class
            );
            return response != null && response.getEvents() != null
                ? response.getEvents()
                : new java.util.ArrayList<>();
        }

        /**
         * Get event logs for a card template without filters.
         */
        public java.util.List<Models.Event> eventLog(String templateId) {
            return eventLog(templateId, null);
        }

        /**
         * Get ledger/billing items.
         */
        public Models.LedgerItemsResult ledgerItems(Models.LedgerItemsParams params) {
            StringBuilder query = new StringBuilder();
            if (params != null) {
                if (params.getPage() != null)
                    appendParam(query, "page", params.getPage().toString());
                if (params.getPerPage() != null)
                    appendParam(query, "per_page", params.getPerPage().toString());
                if (params.getStartDate() != null)
                    appendParam(query, "start_date", params.getStartDate().toString());
                if (params.getEndDate() != null)
                    appendParam(query, "end_date", params.getEndDate().toString());
            }
            Models.LedgerItemsResult result = client.getWithParams(
                "/console/ledger-items",
                query.toString(),
                Models.LedgerItemsResult.class
            );
            return result != null ? result : new Models.LedgerItemsResult();
        }

        /**
         * Get ledger/billing items without filters.
         */
        public Models.LedgerItemsResult ledgerItems() {
            return ledgerItems(null);
        }

        /**
         * iOS In-App Provisioning preflight.
         */
        public Models.IosPreflightResponse iosPreflight(String cardTemplateId, String accessPassExId) {
            String payload = client.serialize(java.util.Map.of("access_pass_ex_id", accessPassExId));
            return client.post("/console/card-templates/" + cardTemplateId + "/ios_preflight", payload, Models.IosPreflightResponse.class);
        }

        /**
         * List all landing pages.
         */
        public java.util.List<Models.LandingPage> listLandingPages() {
            return java.util.Arrays.asList(
                client.getWithParams("/console/landing-pages", "", Models.LandingPage[].class)
            );
        }

        /**
         * Create a new landing page.
         */
        public Models.LandingPage createLandingPage(Models.CreateLandingPageRequest request) {
            String payload = client.serialize(request);
            return client.post("/console/landing-pages", payload, Models.LandingPage.class);
        }

        /**
         * Update an existing landing page.
         */
        public Models.LandingPage updateLandingPage(Models.UpdateLandingPageRequest request) {
            String payload = client.serialize(request);
            return client.patch("/console/landing-pages/" + request.getLandingPageId(), payload, Models.LandingPage.class);
        }

        /**
         * Credential profile operations.
         */
        public CredentialProfilesApi credentialProfiles() {
            return new CredentialProfilesApi(client);
        }

        /**
         * HID-related services.
         */
        public HIDApi hid() {
            return new HIDApi(client);
        }

        private void appendParam(StringBuilder sb, String key, String value) {
            if (sb.length() > 0) sb.append("&");
            sb.append(java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * API for HID-related operations.
     */
    public static class HIDApi {
        private final AccessGridClient client;

        HIDApi(AccessGridClient client) {
            this.client = client;
        }

        /**
         * HID Organizations API.
         */
        public HIDOrgsApi orgs() {
            return new HIDOrgsApi(client);
        }
    }

    /**
     * API for HID Organization operations.
     */
    public static class HIDOrgsApi {
        private final AccessGridClient client;

        HIDOrgsApi(AccessGridClient client) {
            this.client = client;
        }

        /**
         * Create a new HID organization.
         */
        public Models.HIDOrg create(Models.CreateHIDOrgParams params) {
            String payload = client.serialize(params);
            return client.post("/console/hid/orgs", payload, Models.HIDOrg.class);
        }

        /**
         * List all HID organizations.
         */
        public java.util.List<Models.HIDOrg> list() {
            return java.util.Arrays.asList(
                client.getWithParams("/console/hid/orgs", "", Models.HIDOrg[].class)
            );
        }

        /**
         * Complete HID org registration with credentials.
         */
        public Models.HIDOrg activate(Models.CompleteHIDOrgParams params) {
            String payload = client.serialize(params);
            return client.post("/console/hid/orgs/activate", payload, Models.HIDOrg.class);
        }
    }

    /**
     * API for Credential Profile operations.
     */
    public static class CredentialProfilesApi {
        private final AccessGridClient client;

        CredentialProfilesApi(AccessGridClient client) {
            this.client = client;
        }

        /**
         * List all credential profiles.
         */
        public java.util.List<Models.CredentialProfile> list() {
            return java.util.Arrays.asList(
                client.getWithParams("/console/credential-profiles", "", Models.CredentialProfile[].class)
            );
        }

        /**
         * Create a new credential profile.
         */
        public Models.CredentialProfile create(Models.CreateCredentialProfileRequest request) {
            String payload = client.serialize(request);
            return client.post("/console/credential-profiles", payload, Models.CredentialProfile.class);
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
            String encodedPayload = java.net.URLEncoder.encode("{}", StandardCharsets.UTF_8);
            String uri;
            if (queryString == null || queryString.isEmpty()) {
                uri = baseUrl + path + "?sig_payload=" + encodedPayload;
            } else {
                uri = baseUrl + path + "?" + queryString + "&sig_payload=" + encodedPayload;
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
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
