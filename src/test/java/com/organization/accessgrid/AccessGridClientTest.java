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
