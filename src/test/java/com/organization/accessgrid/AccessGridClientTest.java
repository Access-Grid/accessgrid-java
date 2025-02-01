package com.organization.accessgrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Test suite for AccessGridClient.
 * 
 * Note: These are mainly structural tests and mock tests.
 * Actual integration tests would require a live API connection.
 */
public class AccessGridClientTest {
    private AccessGridClient client;

    @BeforeEach
    public void setUp() {
        // Use test credentials - these would typically come from environment variables
        client = new AccessGridClient("test-account-id", "test-secret-key");
    }

    @Test
    public void testClientInitialization() {
        assertNotNull(client, "Client should be initialized");
        assertEquals("test-account-id", client.getAccountId(), "Account ID should match");
    }

    @Test
    public void testProvisionCardRequest() {
        Models.ProvisionCardRequest request = Models.ProvisionCardRequest.builder()
            .cardTemplateId("test-template-123")
            .employeeId("emp-test-456")
            .fullName("Test Employee")
            .email("test.employee@company.com")
            .classification("test")
            .startDate(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
            .expirationDate(ZonedDateTime.now().plusMonths(6).format(DateTimeFormatter.ISO_INSTANT))
            .build();

        assertNotNull(request, "Provision card request should be created");
        assertEquals("test-template-123", request.getCardTemplateId());
        assertEquals("emp-test-456", request.getEmployeeId());
    }

    @Test
    public void testCreateTemplateRequest() {
        Models.CreateTemplateRequest templateRequest = Models.CreateTemplateRequest.builder()
            .name("Test Employee Badge")
            .platform("apple")
            .useCase("employee_identification")
            .protocol("nfc")
            .allowOnMultipleDevices(true)
            .watchCount(2)
            .iphoneCount(3)
            .build();

        assertNotNull(templateRequest, "Create template request should be created");
        assertEquals("Test Employee Badge", templateRequest.getName());
        assertTrue(templateRequest.isAllowOnMultipleDevices());
        assertEquals(2, templateRequest.getWatchCount());
    }

    @Test
    public void testAccessCardsApiCreation() {
        AccessGridClient.AccessCardsApi accessCardsApi = client.accessCards();
        assertNotNull(accessCardsApi, "Access Cards API should be created");
    }

    @Test
    public void testConsoleApiCreation() {
        AccessGridClient.ConsoleApi consoleApi = client.console();
        assertNotNull(consoleApi, "Console API should be created");
    }

    @Test
    public void testAccessGridException() {
        AccessGridClient.AccessGridException exception = 
            new AccessGridClient.AccessGridException("Test error message");
        
        assertNotNull(exception, "Exception should be created");
        assertEquals("Test error message", exception.getMessage());
    }
}