package com.organization.accessgrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

/**
 * Test suite for AccessGridClient.
 * Mocks HttpSender to test at the HTTP transport level.
 */
public class AccessGridClientTest {
    private HttpSender mockSender;
    private AccessGridClient client;

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        mockSender = mock(HttpSender.class);
        client = new AccessGridClient("test-account-id", "test-secret-key", mockSender, "https://api.test.com/v1");
    }

    @SuppressWarnings("unchecked")
    private void mockResponse(String body) throws IOException, InterruptedException {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(mockSender.send(any(HttpRequest.class))).thenReturn(response);
    }

    private HttpRequest captureRequest() throws IOException, InterruptedException {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockSender).send(captor.capture());
        return captor.getValue();
    }

    // --- Client initialization ---

    @Test
    public void testClientInitialization() {
        assertNotNull(client);
        assertEquals("test-account-id", client.getAccountId());
    }

    @Test
    public void testClientRequiresAccountId() {
        assertThrows(AccessGridClient.AccessGridException.class,
            () -> new AccessGridClient(null, "secret"));
    }

    @Test
    public void testClientRequiresSecretKey() {
        assertThrows(AccessGridClient.AccessGridException.class,
            () -> new AccessGridClient("account", null));
    }

    // --- Model builder tests ---

    @Test
    public void testProvisionCardRequestBuilder() {
        Models.ProvisionCardRequest request = Models.ProvisionCardRequest.builder()
            .cardTemplateId("test-template-123")
            .employeeId("emp-test-456")
            .fullName("Test Employee")
            .email("test@company.com")
            .classification("full_time")
            .build();

        assertEquals("test-template-123", request.getCardTemplateId());
        assertEquals("emp-test-456", request.getEmployeeId());
    }

    @Test
    public void testCreateTemplateRequestBuilder() {
        Models.CreateTemplateRequest request = Models.CreateTemplateRequest.builder()
            .name("Test Badge")
            .platform("apple")
            .useCase("employee_badge")
            .allowOnMultipleDevices(true)
            .watchCount(2)
            .iphoneCount(3)
            .build();

        assertEquals("Test Badge", request.getName());
        assertTrue(request.isAllowOnMultipleDevices());
        assertEquals(2, request.getWatchCount());
    }

    // --- Access Cards API ---

    @Test
    public void testProvisionCardSendsPostToKeyCards() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"card-123\",\"install_url\":\"https://example.com/install\",\"state\":\"active\"}");

        Models.ProvisionCardRequest request = Models.ProvisionCardRequest.builder()
            .cardTemplateId("template-123")
            .fullName("John Doe")
            .email("john@example.com")
            .build();

        Models.Card card = client.accessCards().provision(request);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/key-cards"), "Should POST to /key-cards, got: " + captured.uri().getPath());
        assertEquals("POST", captured.method());
        assertEquals("card-123", card.getId());
        assertEquals("https://example.com/install", card.getInstallUrl());
    }

    @Test
    public void testGetCardSendsGetToKeyCards() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"card-456\",\"state\":\"active\",\"full_name\":\"Jane Doe\",\"card_number\":\"12345\",\"site_code\":\"100\"}");

        Models.Card card = client.accessCards().get("card-456");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/key-cards/card-456"), "Should GET /key-cards/{id}");
        assertEquals("GET", captured.method());
        assertEquals("card-456", card.getId());
        assertEquals("Jane Doe", card.getFullName());
    }

    @Test
    public void testUpdateCardSendsPatchToKeyCards() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"card-789\",\"state\":\"active\",\"full_name\":\"Updated Name\"}");

        Models.UpdateCardRequest request = Models.UpdateCardRequest.builder()
            .cardId("card-789")
            .fullName("Updated Name")
            .title("Senior Developer")
            .build();

        Models.Card card = client.accessCards().update(request);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/key-cards/card-789"), "Should PATCH /key-cards/{id}");
        assertEquals("PATCH", captured.method());
        assertEquals("Updated Name", card.getFullName());
    }

    @Test
    public void testListCardsSendsGetToKeyCards() throws IOException, InterruptedException {
        mockResponse("{\"keys\":[{\"id\":\"card-1\",\"state\":\"active\"},{\"id\":\"card-2\",\"state\":\"suspended\"}]}");

        java.util.List<Models.Card> cards = client.accessCards().list();

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/key-cards"), "Should GET /key-cards");
        assertEquals("GET", captured.method());
        assertEquals(2, cards.size());
        assertEquals("card-1", cards.get(0).getId());
    }

    @Test
    public void testListCardsWithFiltersSendsQueryParams() throws IOException, InterruptedException {
        mockResponse("{\"keys\":[{\"id\":\"card-1\",\"state\":\"active\"}]}");

        Models.ListKeysParams params = Models.ListKeysParams.builder()
            .templateId("tmpl-1")
            .state("active")
            .build();

        java.util.List<Models.Card> cards = client.accessCards().list(params);

        HttpRequest captured = captureRequest();
        String query = captured.uri().getQuery();
        assertNotNull(query);
        assertTrue(query.contains("template_id=tmpl-1"), "Should include template_id param");
        assertTrue(query.contains("state=active"), "Should include state param");
    }

    @Test
    public void testSuspendCardSendsPostToSuspend() throws IOException, InterruptedException {
        mockResponse("{}");
        client.accessCards().suspend("card-123");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/key-cards/card-123/suspend"), "Should POST to /key-cards/{id}/suspend");
        assertEquals("POST", captured.method());
    }

    @Test
    public void testResumeCardSendsPostToResume() throws IOException, InterruptedException {
        mockResponse("{}");
        client.accessCards().resume("card-123");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/key-cards/card-123/resume"), "Should POST to /key-cards/{id}/resume");
        assertEquals("POST", captured.method());
    }

    @Test
    public void testUnlinkCardSendsPostToUnlink() throws IOException, InterruptedException {
        mockResponse("{}");
        client.accessCards().unlink("card-123");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/key-cards/card-123/unlink"), "Should POST to /key-cards/{id}/unlink");
        assertEquals("POST", captured.method());
    }

    @Test
    public void testDeleteCardSendsPostToDelete() throws IOException, InterruptedException {
        mockResponse("{}");
        client.accessCards().delete("card-123");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/key-cards/card-123/delete"), "Should POST to /key-cards/{id}/delete");
        assertEquals("POST", captured.method());
    }

    // --- Console API ---

    @Test
    public void testCreateTemplateSendsPostToConsoleCardTemplates() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"tmpl-789\",\"name\":\"Test Template\",\"platform\":\"apple\"}");

        Models.CreateTemplateRequest request = Models.CreateTemplateRequest.builder()
            .name("Test Template")
            .platform("apple")
            .useCase("employee_badge")
            .build();

        Models.Template template = client.console().createTemplate(request);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/console/card-templates"), "Should POST to /console/card-templates");
        assertEquals("POST", captured.method());
        assertEquals("tmpl-789", template.getId());
    }

    // --- Response deserialization ---

    @Test
    public void testCardDeserialization() throws IOException, InterruptedException {
        String json = "{" +
            "\"id\":\"card-100\"," +
            "\"state\":\"active\"," +
            "\"full_name\":\"Alice Smith\"," +
            "\"install_url\":\"https://example.com/install\"," +
            "\"direct_install_url\":\"https://example.com/direct\"," +
            "\"card_number\":\"54321\"," +
            "\"site_code\":\"200\"," +
            "\"expiration_date\":\"2026-12-31\"," +
            "\"card_template_id\":\"tmpl-1\"," +
            "\"devices\":[{\"id\":\"dev-1\",\"platform\":\"ios\",\"status\":\"active\"}]," +
            "\"metadata\":{\"dept\":\"eng\"}" +
            "}";
        mockResponse(json);

        Models.Card card = client.accessCards().get("card-100");

        assertEquals("card-100", card.getId());
        assertEquals("active", card.getState());
        assertEquals("Alice Smith", card.getFullName());
        assertEquals("https://example.com/install", card.getInstallUrl());
        assertEquals("https://example.com/install", card.getUrl());
        assertEquals("https://example.com/direct", card.getDirectInstallUrl());
        assertEquals("54321", card.getCardNumber());
        assertEquals("200", card.getSiteCode());
        assertEquals("2026-12-31", card.getExpirationDate());
        assertEquals("tmpl-1", card.getCardTemplateId());
        assertEquals(1, card.getDevices().size());
        assertEquals("dev-1", card.getDevices().get(0).getId());
        assertEquals("eng", card.getMetadata().get("dept"));
    }

    // --- Console: Update Template ---

    @Test
    public void testUpdateTemplateSendsPutToConsoleCardTemplates() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"tmpl-1\",\"name\":\"Updated Template\"}");

        Models.UpdateTemplateRequest request = Models.UpdateTemplateRequest.builder()
            .cardTemplateId("tmpl-1")
            .name("Updated Template")
            .backgroundColor("#FFFFFF")
            .build();

        Models.Template template = client.console().updateTemplate(request);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/console/card-templates/tmpl-1"), "Should PUT to /console/card-templates/{id}");
        assertEquals("PUT", captured.method());
        assertEquals("Updated Template", template.getName());
    }

    // --- Console: Read Template ---

    @Test
    public void testReadTemplateSendsGetToConsoleCardTemplates() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"tmpl-1\",\"name\":\"My Template\",\"platform\":\"apple\",\"protocol\":\"desfire\",\"allow_on_multiple_devices\":true}");

        Models.Template template = client.console().readTemplate("tmpl-1");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/console/card-templates/tmpl-1"), "Should GET /console/card-templates/{id}");
        assertEquals("GET", captured.method());
        assertEquals("tmpl-1", template.getId());
        assertEquals("My Template", template.getName());
        assertTrue(template.isAllowOnMultipleDevices());
    }

    // --- Console: Publish Template ---

    @Test
    public void testPublishTemplateSendsPostToPublishEndpoint() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"tmpl-1\",\"status\":\"in-review\"}");

        Models.PublishTemplateResponse response = client.console().publishTemplate("tmpl-1");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/console/card-templates/tmpl-1/publish"),
            "Should POST /console/card-templates/{id}/publish");
        assertEquals("POST", captured.method());
        assertEquals("tmpl-1", response.getId());
        assertEquals("in-review", response.getStatus());
    }

    // --- Console: Reveal Smart Tap Private Key ---

    @Test
    public void testRevealTemplatePrivateKeyDecryptsServerEnvelope() throws Exception {
        final String plaintextPem =
            "-----BEGIN EC PRIVATE KEY-----\n" +
            "MHcCAQEEIBmlx2KqB7+RLMrHWLMm6hh3JwFrL2ZxZTLkW1yX8OabAoGCCqGSM49\n" +
            "AwEHoUQDQgAEs5bJrjEXAMPLEEXAMPLEEXAMPLEEXAMPLEEXAMPLEEXAMPLEEXA\n" +
            "MPLEEXAMPLEEXAMPLEEXAMPLEEXAMPLEEXAMPLEEXAMPLEEXAMPLEE=\n" +
            "-----END EC PRIVATE KEY-----\n";

        // Capture the SDK's outgoing client_public_key, then encrypt against it.
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(mockSender.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest sent = invocation.getArgument(0);
            String requestBody = bodyOf(sent);
            String clientPublicKeyPem = client.objectMapper.readTree(requestBody)
                .get("client_public_key").asText();

            FakeServerEnvelope envelope = simulateServerEncrypt(plaintextPem, clientPublicKeyPem);
            String responseJson = client.objectMapper.writeValueAsString(java.util.Map.of(
                "key_version", "tmpl-42",
                "collector_id", "12345678",
                "fingerprint", "a".repeat(64),
                "encrypted_private_key", java.util.Map.of(
                    "alg", "ECDH-ES+A256GCM",
                    "ephemeral_public_key", envelope.ephemeralPublicKeyPem,
                    "iv", java.util.Base64.getEncoder().encodeToString(envelope.iv),
                    "ciphertext", java.util.Base64.getEncoder().encodeToString(envelope.ciphertext),
                    "tag", java.util.Base64.getEncoder().encodeToString(envelope.tag)
                )
            ));
            when(response.body()).thenReturn(responseJson);
            return response;
        });

        Models.RevealTemplatePrivateKeyResponse result =
            client.console().revealTemplatePrivateKey("tmpl-42");

        assertEquals("tmpl-42", result.getKeyVersion());
        assertEquals("12345678", result.getCollectorId());
        assertEquals(64, result.getFingerprint().length());
        assertEquals(plaintextPem, result.getPrivateKey());

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/console/card-templates/tmpl-42/smart-tap/reveal"),
            "Should POST to smart-tap/reveal");
        assertEquals("POST", captured.method());
    }

    @Test
    public void testRevealTemplatePrivateKeyRejectsEmptyTemplateId() {
        assertThrows(AccessGridClient.AccessGridException.class,
            () -> client.console().revealTemplatePrivateKey(""));
    }

    private static String bodyOf(HttpRequest request) {
        return request.bodyPublisher()
            .map(p -> {
                java.util.concurrent.Flow.Subscriber<? super java.nio.ByteBuffer> noop;
                java.util.List<java.nio.ByteBuffer> received = new java.util.ArrayList<>();
                java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
                p.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
                    public void onSubscribe(java.util.concurrent.Flow.Subscription s) { s.request(Long.MAX_VALUE); }
                    public void onNext(java.nio.ByteBuffer b) { received.add(b); }
                    public void onError(Throwable t) { done.countDown(); }
                    public void onComplete() { done.countDown(); }
                });
                try { done.await(); } catch (InterruptedException ignored) {}
                int total = received.stream().mapToInt(java.nio.ByteBuffer::remaining).sum();
                byte[] all = new byte[total];
                int offset = 0;
                for (java.nio.ByteBuffer buf : received) {
                    int n = buf.remaining();
                    buf.get(all, offset, n);
                    offset += n;
                }
                return new String(all, java.nio.charset.StandardCharsets.UTF_8);
            })
            .orElse("");
    }

    private static class FakeServerEnvelope {
        String ephemeralPublicKeyPem;
        byte[] iv;
        byte[] ciphertext;
        byte[] tag;
    }

    // Mirrors rails' SmartTap::RevealEncryption.encrypt using BouncyCastle so the
    // test exercises the SDK's full ECDH/HKDF/AES-GCM round-trip.
    private static FakeServerEnvelope simulateServerEncrypt(String plaintextPem, String clientPublicKeyPem) throws Exception {
        org.bouncycastle.asn1.x9.X9ECParameters curve =
            org.bouncycastle.asn1.x9.ECNamedCurveTable.getByName("P-256");
        org.bouncycastle.crypto.params.ECDomainParameters domain =
            new org.bouncycastle.crypto.params.ECDomainParameters(
                curve.getCurve(), curve.getG(), curve.getN(), curve.getH(), curve.getSeed());

        org.bouncycastle.crypto.generators.ECKeyPairGenerator gen =
            new org.bouncycastle.crypto.generators.ECKeyPairGenerator();
        gen.init(new org.bouncycastle.crypto.params.ECKeyGenerationParameters(
            domain, new java.security.SecureRandom()));
        org.bouncycastle.crypto.AsymmetricCipherKeyPair ephemeral = gen.generateKeyPair();

        org.bouncycastle.crypto.params.ECPublicKeyParameters clientPub;
        try (org.bouncycastle.openssl.PEMParser parser =
                new org.bouncycastle.openssl.PEMParser(new java.io.StringReader(clientPublicKeyPem))) {
            Object o = parser.readObject();
            clientPub = (org.bouncycastle.crypto.params.ECPublicKeyParameters)
                org.bouncycastle.crypto.util.PublicKeyFactory.createKey(
                    (org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) o);
        }

        org.bouncycastle.crypto.agreement.ECDHBasicAgreement agreement =
            new org.bouncycastle.crypto.agreement.ECDHBasicAgreement();
        agreement.init(ephemeral.getPrivate());
        java.math.BigInteger sharedBig = agreement.calculateAgreement(clientPub);
        byte[] unsigned = sharedBig.toByteArray();
        byte[] sharedSecret = new byte[32];
        if (unsigned.length == 33 && unsigned[0] == 0) {
            System.arraycopy(unsigned, 1, sharedSecret, 0, 32);
        } else {
            System.arraycopy(unsigned, 0, sharedSecret, 32 - unsigned.length, unsigned.length);
        }

        org.bouncycastle.crypto.generators.HKDFBytesGenerator hkdf =
            new org.bouncycastle.crypto.generators.HKDFBytesGenerator(
                new org.bouncycastle.crypto.digests.SHA256Digest());
        hkdf.init(new org.bouncycastle.crypto.params.HKDFParameters(
            sharedSecret, new byte[0],
            "accessgrid-smart-tap-reveal-v1".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        byte[] aesKey = new byte[32];
        hkdf.generateBytes(aesKey, 0, aesKey.length);

        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        org.bouncycastle.crypto.modes.GCMBlockCipher gcm =
            new org.bouncycastle.crypto.modes.GCMBlockCipher(
                new org.bouncycastle.crypto.engines.AESEngine());
        gcm.init(true, new org.bouncycastle.crypto.params.AEADParameters(
            new org.bouncycastle.crypto.params.KeyParameter(aesKey),
            128, iv, new byte[0]));
        byte[] plaintextBytes = plaintextPem.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] combined = new byte[gcm.getOutputSize(plaintextBytes.length)];
        int written = gcm.processBytes(plaintextBytes, 0, plaintextBytes.length, combined, 0);
        written += gcm.doFinal(combined, written);
        byte[] ciphertext = new byte[combined.length - 16];
        byte[] tag = new byte[16];
        System.arraycopy(combined, 0, ciphertext, 0, ciphertext.length);
        System.arraycopy(combined, ciphertext.length, tag, 0, 16);

        org.bouncycastle.asn1.x509.SubjectPublicKeyInfo spki =
            org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(ephemeral.getPublic());
        java.io.StringWriter sw = new java.io.StringWriter();
        try (org.bouncycastle.openssl.jcajce.JcaPEMWriter w = new org.bouncycastle.openssl.jcajce.JcaPEMWriter(sw)) {
            w.writeObject(spki);
        }

        FakeServerEnvelope env = new FakeServerEnvelope();
        env.ephemeralPublicKeyPem = sw.toString();
        env.iv = iv;
        env.ciphertext = ciphertext;
        env.tag = tag;
        return env;
    }

    // --- Console: Event Log ---

    @Test
    public void testEventLogSendsGetToLogs() throws IOException, InterruptedException {
        mockResponse("{\"events\":[{\"type\":\"install\",\"user_id\":\"user-1\"}]}");

        java.util.List<Models.Event> events = client.console().eventLog("tmpl-1");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/console/card-templates/tmpl-1/logs"), "Should GET /console/card-templates/{id}/logs");
        assertEquals("GET", captured.method());
        assertEquals(1, events.size());
        assertEquals("install", events.get(0).getType());
    }

    @Test
    public void testEventLogWithFiltersSendsQueryParams() throws IOException, InterruptedException {
        mockResponse("{\"events\":[]}");

        Models.EventLogFilters filters = Models.EventLogFilters.builder()
            .device("mobile")
            .eventType("install")
            .build();

        java.util.List<Models.Event> events = client.console().eventLog("tmpl-1", filters);

        HttpRequest captured = captureRequest();
        String query = captured.uri().getQuery();
        assertTrue(query != null && query.contains("device=mobile"), "Should include device param");
        assertTrue(query != null && query.contains("event_type=install"), "Should include event_type param");
    }

    // --- Console: Ledger Items ---

    @Test
    public void testLedgerItemsSendsGetToLedgerItems() throws IOException, InterruptedException {
        mockResponse("{\"ledger_items\":[{\"id\":\"li-1\",\"amount\":\"5.00\",\"kind\":\"provision\",\"created_at\":\"2026-01-01\",\"access_pass\":{\"ex_id\":\"ap-1\",\"pass_template\":{\"ex_id\":\"pt-1\"}}}],\"pagination\":{\"current_page\":1,\"total_pages\":1,\"total_count\":1,\"per_page\":50}}");

        Models.LedgerItemsResult result = client.console().ledgerItems();

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/console/ledger-items"), "Should GET /console/ledger-items");
        assertEquals(1, result.getLedgerItems().size());
        assertEquals("5.00", result.getLedgerItems().get(0).getAmount());
        assertEquals("provision", result.getLedgerItems().get(0).getKind());
        assertEquals("ap-1", result.getLedgerItems().get(0).getAccessPass().getExId());
        assertEquals("pt-1", result.getLedgerItems().get(0).getAccessPass().getPassTemplate().getExId());
    }

    // --- Console: iOS Preflight ---

    @Test
    public void testIosPreflightSendsPostToIosPreflight() throws IOException, InterruptedException {
        mockResponse("{\"provisioningCredentialIdentifier\":\"pci-1\",\"sharingInstanceIdentifier\":\"sii-1\",\"cardTemplateIdentifier\":\"cti-1\",\"environmentIdentifier\":\"ei-1\"}");

        Models.IosPreflightResponse response = client.console().iosPreflight("tmpl-1", "ap-ex-1");

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/console/card-templates/tmpl-1/ios_preflight"), "Should POST to ios_preflight");
        assertEquals("POST", captured.method());
        assertEquals("pci-1", response.getProvisioningCredentialIdentifier());
        assertEquals("sii-1", response.getSharingInstanceIdentifier());
    }

    // --- Console: HID Orgs ---

    @Test
    public void testHIDOrgsCreateSendsPostToHidOrgs() throws IOException, InterruptedException {
        mockResponse("{\"id\":1,\"name\":\"My Org\",\"slug\":\"my-org\",\"first_name\":\"Ada\",\"last_name\":\"Lovelace\",\"status\":\"pending\"}");

        Models.CreateHIDOrgParams params = Models.CreateHIDOrgParams.builder()
            .name("My Org")
            .fullAddress("1 Main St, NY NY")
            .phone("+1-555-0000")
            .firstName("Ada")
            .lastName("Lovelace")
            .build();

        Models.HIDOrg org = client.console().hid().orgs().create(params);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/console/hid/orgs"), "Should POST to /console/hid/orgs");
        assertEquals("POST", captured.method());
        assertEquals("My Org", org.getName());
        assertEquals("my-org", org.getSlug());
    }

    @Test
    public void testHIDOrgsListSendsGetToHidOrgs() throws IOException, InterruptedException {
        mockResponse("[{\"id\":1,\"name\":\"Org 1\",\"slug\":\"org-1\"},{\"id\":2,\"name\":\"Org 2\",\"slug\":\"org-2\"}]");

        java.util.List<Models.HIDOrg> orgs = client.console().hid().orgs().list();

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/console/hid/orgs"), "Should GET /console/hid/orgs");
        assertEquals("GET", captured.method());
        assertEquals(2, orgs.size());
        assertEquals("Org 1", orgs.get(0).getName());
    }

    @Test
    public void testHIDOrgsActivateSendsPostToActivate() throws IOException, InterruptedException {
        mockResponse("{\"id\":1,\"name\":\"My Org\",\"slug\":\"my-org\",\"status\":\"active\"}");

        Models.CompleteHIDOrgParams params = Models.CompleteHIDOrgParams.builder()
            .email("admin@example.com")
            .password("hid-password-123")
            .build();

        Models.HIDOrg org = client.console().hid().orgs().activate(params);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/console/hid/orgs/activate"), "Should POST to /console/hid/orgs/activate");
        assertEquals("POST", captured.method());
        assertEquals("active", org.getStatus());
    }

    // --- Template params serialization ---

    @Test
    public void testCreateTemplateSerializesWithFlatParams() throws Exception {
        Models.CreateTemplateRequest request = Models.CreateTemplateRequest.builder()
            .name("Employee Access Pass")
            .platform("apple")
            .useCase("employee_badge")
            .protocol("desfire")
            .allowOnMultipleDevices(true)
            .watchCount(2)
            .iphoneCount(3)
            .backgroundColor("#FFFFFF")
            .labelColor("#000000")
            .labelSecondaryColor("#333333")
            .supportUrl("https://help.example.com")
            .supportPhoneNumber("+1-555-123-4567")
            .supportEmail("support@example.com")
            .privacyPolicyUrl("https://example.com/privacy")
            .termsAndConditionsUrl("https://example.com/terms")
            .metadata(java.util.Map.of("version", "2.1"))
            .build();

        String json = client.objectMapper.writeValueAsString(request);

        // Flat params at root level
        assertTrue(json.contains("\"background_color\":\"#FFFFFF\""), "background_color should be flat");
        assertTrue(json.contains("\"label_color\":\"#000000\""), "label_color should be flat");
        assertTrue(json.contains("\"support_url\":\"https://help.example.com\""), "support_url should be flat");
        assertTrue(json.contains("\"support_email\":\"support@example.com\""), "support_email should be flat");
        assertTrue(json.contains("\"privacy_policy_url\":\"https://example.com/privacy\""), "privacy_policy_url should be flat");
        assertTrue(json.contains("\"metadata\":{\"version\":\"2.1\"}"), "metadata should be present");

        // No nested objects
        assertFalse(json.contains("\"design\""), "Should not have nested design object");
        assertFalse(json.contains("\"support_info\""), "Should not have nested support_info object");
    }

    @Test
    public void testUpdateTemplateSerializesWithFlatParams() throws Exception {
        Models.UpdateTemplateRequest request = Models.UpdateTemplateRequest.builder()
            .cardTemplateId("tmpl-123")
            .name("Updated Pass")
            .backgroundColor("#FFFFFF")
            .labelColor("#000000")
            .supportUrl("https://help.example.com")
            .supportEmail("support@example.com")
            .build();

        String json = client.objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"background_color\":\"#FFFFFF\""), "background_color should be flat");
        assertTrue(json.contains("\"support_url\":\"https://help.example.com\""), "support_url should be flat");
        assertFalse(json.contains("\"design\""), "Should not have nested design object");
        assertFalse(json.contains("\"support_info\""), "Should not have nested support_info object");
    }

    @Test
    public void testProvisionCardRequestIncludesTitleAndMetadata() throws Exception {
        Models.ProvisionCardRequest request = Models.ProvisionCardRequest.builder()
            .cardTemplateId("tmpl-1")
            .fullName("John Doe")
            .title("Engineering Manager")
            .metadata(java.util.Map.of("department", "engineering"))
            .build();

        String json = client.objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"title\":\"Engineering Manager\""), "title should be present");
        assertTrue(json.contains("\"department\":\"engineering\""), "metadata should be present");
    }

    // --- Auth headers ---

    @Test
    public void testRequestIncludesAuthHeaders() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"card-1\"}");

        client.accessCards().provision(Models.ProvisionCardRequest.builder()
            .cardTemplateId("t1").fullName("X").build());

        HttpRequest captured = captureRequest();
        assertEquals("test-account-id", captured.headers().firstValue("X-ACCT-ID").orElse(null));
        assertNotNull(captured.headers().firstValue("X-PAYLOAD-SIG").orElse(null));
        assertTrue(captured.headers().firstValue("User-Agent").orElse("").startsWith("accessgrid.java/"));
    }

    // --- Error handling ---

    // --- Card model: new fields ---

    @Test
    public void testCardDeserializationWithNewFields() throws IOException, InterruptedException {
        String json = "{" +
            "\"id\":\"card-200\"," +
            "\"state\":\"active\"," +
            "\"full_name\":\"Bob Smith\"," +
            "\"organization_name\":\"Acme Corp\"," +
            "\"department\":\"Engineering\"," +
            "\"location\":\"San Francisco\"," +
            "\"site_name\":\"HQ Building A\"," +
            "\"workstation\":\"4F-207\"," +
            "\"mail_stop\":\"MS-401\"," +
            "\"company_address\":\"123 Main St, San Francisco, CA 94105\"," +
            "\"install_url\":\"https://example.com/install\"" +
            "}";
        mockResponse(json);

        Models.Card card = client.accessCards().get("card-200");

        assertEquals("Acme Corp", card.getOrganizationName());
        assertEquals("Engineering", card.getDepartment());
        assertEquals("San Francisco", card.getLocation());
        assertEquals("HQ Building A", card.getSiteName());
        assertEquals("4F-207", card.getWorkstation());
        assertEquals("MS-401", card.getMailStop());
        assertEquals("123 Main St, San Francisco, CA 94105", card.getCompanyAddress());
    }

    @Test
    public void testCardNewFieldsNullWhenAbsent() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"card-201\",\"state\":\"active\"}");

        Models.Card card = client.accessCards().get("card-201");

        assertNull(card.getOrganizationName());
        assertNull(card.getDepartment());
        assertNull(card.getLocation());
        assertNull(card.getSiteName());
        assertNull(card.getWorkstation());
        assertNull(card.getMailStop());
        assertNull(card.getCompanyAddress());
    }

    @Test
    public void testProvisionRequestSerializesNewFields() throws Exception {
        Models.ProvisionCardRequest request = Models.ProvisionCardRequest.builder()
            .cardTemplateId("tmpl-1")
            .fullName("John Doe")
            .department("Engineering")
            .location("San Francisco")
            .siteName("HQ Building A")
            .workstation("4F-207")
            .mailStop("MS-401")
            .companyAddress("123 Main St, San Francisco, CA 94105")
            .organizationName("Acme Corp")
            .build();

        String json = client.objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"department\":\"Engineering\""));
        assertTrue(json.contains("\"location\":\"San Francisco\""));
        assertTrue(json.contains("\"site_name\":\"HQ Building A\""));
        assertTrue(json.contains("\"workstation\":\"4F-207\""));
        assertTrue(json.contains("\"mail_stop\":\"MS-401\""));
        assertTrue(json.contains("\"company_address\":\"123 Main St, San Francisco, CA 94105\""));
        assertTrue(json.contains("\"organization_name\":\"Acme Corp\""));
    }

    @Test
    public void testUpdateRequestSerializesNewFields() throws Exception {
        Models.UpdateCardRequest request = Models.UpdateCardRequest.builder()
            .cardId("card-1")
            .department("Marketing")
            .location("New York")
            .siteName("NYC Office")
            .workstation("2F-105")
            .mailStop("MS-200")
            .companyAddress("456 Broadway, New York, NY 10013")
            .build();

        String json = client.objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"department\":\"Marketing\""));
        assertTrue(json.contains("\"location\":\"New York\""));
        assertTrue(json.contains("\"site_name\":\"NYC Office\""));
        assertTrue(json.contains("\"workstation\":\"2F-105\""));
        assertTrue(json.contains("\"mail_stop\":\"MS-200\""));
        assertTrue(json.contains("\"company_address\":\"456 Broadway, New York, NY 10013\""));
    }

    // --- Console: Landing Pages ---

    @Test
    public void testListLandingPagesSendsGetToLandingPages() throws IOException, InterruptedException {
        mockResponse("[{\"id\":\"lp-1\",\"name\":\"Office Pass\",\"kind\":\"universal\",\"password_protected\":false,\"logo_url\":\"https://example.com/logo.png\",\"created_at\":\"2026-01-01\"},{\"id\":\"lp-2\",\"name\":\"VIP Pass\",\"kind\":\"universal\",\"password_protected\":true}]");

        java.util.List<Models.LandingPage> pages = client.console().listLandingPages();

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/console/landing-pages"), "Should GET /console/landing-pages");
        assertEquals("GET", captured.method());
        assertEquals(2, pages.size());
        assertEquals("lp-1", pages.get(0).getId());
        assertEquals("Office Pass", pages.get(0).getName());
        assertEquals("universal", pages.get(0).getKind());
        assertFalse(pages.get(0).isPasswordProtected());
        assertEquals("https://example.com/logo.png", pages.get(0).getLogoUrl());
        assertTrue(pages.get(1).isPasswordProtected());
    }

    @Test
    public void testListLandingPagesEmpty() throws IOException, InterruptedException {
        mockResponse("[]");

        java.util.List<Models.LandingPage> pages = client.console().listLandingPages();

        assertEquals(0, pages.size());
    }

    @Test
    public void testCreateLandingPageSendsPostToLandingPages() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"lp-new\",\"name\":\"Miami Office\",\"kind\":\"universal\",\"password_protected\":false}");

        Models.CreateLandingPageRequest request = Models.CreateLandingPageRequest.builder()
            .name("Miami Office")
            .kind("universal")
            .additionalText("Welcome to the Miami Office")
            .bgColor("#f1f5f9")
            .allowImmediateDownload(true)
            .build();

        Models.LandingPage page = client.console().createLandingPage(request);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/console/landing-pages"), "Should POST to /console/landing-pages");
        assertEquals("POST", captured.method());
        assertEquals("lp-new", page.getId());
        assertEquals("Miami Office", page.getName());
    }

    @Test
    public void testUpdateLandingPageSendsPatchToLandingPages() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"lp-1\",\"name\":\"Updated Page\",\"kind\":\"universal\",\"password_protected\":false}");

        Models.UpdateLandingPageRequest request = Models.UpdateLandingPageRequest.builder()
            .landingPageId("lp-1")
            .name("Updated Page")
            .additionalText("Updated text")
            .bgColor("#e2e8f0")
            .build();

        Models.LandingPage page = client.console().updateLandingPage(request);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().contains("/console/landing-pages/lp-1"), "Should PUT /console/landing-pages/{id}");
        assertEquals("PUT", captured.method());
        assertEquals("Updated Page", page.getName());
    }

    // --- Console: Credential Profiles ---

    @Test
    public void testListCredentialProfilesSendsGetToCredentialProfiles() throws IOException, InterruptedException {
        mockResponse("[{\"id\":\"cp-1\",\"name\":\"Main Profile\",\"aid\":\"F0010203040506\",\"apple_id\":\"pass.com.example\",\"created_at\":\"2026-01-01\",\"card_storage\":\"2000\",\"keys\":[{\"label\":\"Master Key\",\"value\":\"AABBCCDD\",\"ex_id\":\"k-1\"}],\"files\":[{\"ex_id\":\"f-1\",\"communication_settings\":\"plain\",\"read_rights\":\"key0\",\"write_rights\":\"key0\",\"read_write_rights\":\"key0\",\"change_rights\":\"key0\"}]}]");

        java.util.List<Models.CredentialProfile> profiles = client.console().credentialProfiles().list();

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/console/credential-profiles"), "Should GET /console/credential-profiles");
        assertEquals("GET", captured.method());
        assertEquals(1, profiles.size());
        assertEquals("cp-1", profiles.get(0).getId());
        assertEquals("Main Profile", profiles.get(0).getName());
        assertEquals("F0010203040506", profiles.get(0).getAid());
        assertEquals("pass.com.example", profiles.get(0).getAppleId());
        assertEquals("2000", profiles.get(0).getCardStorage());
        assertEquals(1, profiles.get(0).getKeys().size());
        assertEquals("Master Key", profiles.get(0).getKeys().get(0).getLabel());
        assertEquals(1, profiles.get(0).getFiles().size());
        assertEquals("f-1", profiles.get(0).getFiles().get(0).getExId());
    }

    @Test
    public void testListCredentialProfilesEmpty() throws IOException, InterruptedException {
        mockResponse("[]");

        java.util.List<Models.CredentialProfile> profiles = client.console().credentialProfiles().list();

        assertEquals(0, profiles.size());
    }

    @Test
    public void testCreateCredentialProfileSendsPostToCredentialProfiles() throws IOException, InterruptedException {
        mockResponse("{\"id\":\"cp-new\",\"name\":\"New Profile\",\"aid\":\"F0010203040506\"}");

        Models.CreateCredentialProfileRequest request = Models.CreateCredentialProfileRequest.builder()
            .name("New Profile")
            .appName("KEY-ID-main")
            .keys(java.util.List.of(
                new Models.KeyParam("your_32_char_hex_master_key_here"),
                new Models.KeyParam("your_32_char_hex__read_key__here")
            ))
            .build();

        Models.CredentialProfile profile = client.console().credentialProfiles().create(request);

        HttpRequest captured = captureRequest();
        assertTrue(captured.uri().getPath().endsWith("/console/credential-profiles"), "Should POST to /console/credential-profiles");
        assertEquals("POST", captured.method());
        assertEquals("cp-new", profile.getId());
        assertEquals("New Profile", profile.getName());
        assertEquals("F0010203040506", profile.getAid());
    }

    @Test
    public void testCreateCredentialProfileSerializesKeys() throws Exception {
        Models.CreateCredentialProfileRequest request = Models.CreateCredentialProfileRequest.builder()
            .name("Test Profile")
            .appName("KEY-ID-test")
            .keys(java.util.List.of(
                new Models.KeyParam("hex_key_1"),
                new Models.KeyParam("hex_key_2")
            ))
            .build();

        String json = client.objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"app_name\":\"KEY-ID-test\""), "app_name should be serialized");
        assertTrue(json.contains("\"hex_key_1\""), "key values should be serialized");
        assertTrue(json.contains("\"hex_key_2\""), "key values should be serialized");
    }

    // --- Error handling ---

    @Test
    @SuppressWarnings("unchecked")
    public void testNon200ResponseThrowsException() throws IOException, InterruptedException {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(401);
        when(response.body()).thenReturn("{\"error\":\"unauthorized\"}");
        when(mockSender.send(any(HttpRequest.class))).thenReturn(response);

        assertThrows(AccessGridClient.AccessGridException.class,
            () -> client.accessCards().get("card-1"));
    }
}
