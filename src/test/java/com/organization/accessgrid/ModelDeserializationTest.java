package com.organization.accessgrid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that JSON API responses deserialize correctly into model classes.
 * Uses the same ObjectMapper configuration as the production client.
 */
public class ModelDeserializationTest {
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void cardBasicFields() throws Exception {
        String json = "{"
            + "\"id\": \"0xc4rd1d\","
            + "\"card_template_id\": \"0xd3adb00b5\","
            + "\"full_name\": \"Employee Name\","
            + "\"state\": \"active\","
            + "\"card_number\": \"12345\","
            + "\"site_code\": \"001\","
            + "\"expiration_date\": \"2025-12-31T00:00:00Z\""
            + "}";

        Models.Card card = mapper.readValue(json, Models.Card.class);

        assertEquals("0xc4rd1d", card.getId());
        assertEquals("0xd3adb00b5", card.getCardTemplateId());
        assertEquals("Employee Name", card.getFullName());
        assertEquals("active", card.getState());
        assertEquals("12345", card.getCardNumber());
        assertEquals("001", card.getSiteCode());
        assertEquals("2025-12-31T00:00:00Z", card.getExpirationDate());
    }

    @Test
    public void cardInstallUrl() throws Exception {
        String json = "{"
            + "\"id\": \"0xc4rd1d\","
            + "\"install_url\": \"https://accessgrid.com/install/0xc4rd1d\","
            + "\"direct_install_url\": \"https://accessgrid.com/direct/0xc4rd1d\""
            + "}";

        Models.Card card = mapper.readValue(json, Models.Card.class);

        // install_url is mapped to both 'url' and 'installUrl' fields via
        // duplicate @JsonProperty("install_url") — verify what actually happens
        assertEquals("https://accessgrid.com/install/0xc4rd1d", card.getInstallUrl());
        assertEquals("https://accessgrid.com/install/0xc4rd1d", card.getUrl());
        assertEquals("https://accessgrid.com/direct/0xc4rd1d", card.getDirectInstallUrl());
    }

    @Test
    public void cardWithDevices() throws Exception {
        String json = "{"
            + "\"id\": \"0xc4rd1d\","
            + "\"state\": \"active\","
            + "\"devices\": ["
            + "  {"
            + "    \"id\": \"dev_001\","
            + "    \"platform\": \"apple\","
            + "    \"device_type\": \"iphone\","
            + "    \"status\": \"active\","
            + "    \"created_at\": \"2024-01-01T00:00:00Z\","
            + "    \"updated_at\": \"2024-06-01T00:00:00Z\""
            + "  },"
            + "  {"
            + "    \"id\": \"dev_002\","
            + "    \"platform\": \"apple\","
            + "    \"device_type\": \"watch\","
            + "    \"status\": \"active\","
            + "    \"created_at\": \"2024-02-01T00:00:00Z\","
            + "    \"updated_at\": \"2024-06-15T00:00:00Z\""
            + "  }"
            + "]"
            + "}";

        Models.Card card = mapper.readValue(json, Models.Card.class);

        assertNotNull(card.getDevices());
        assertEquals(2, card.getDevices().size());

        Models.Device device = card.getDevices().get(0);
        assertEquals("dev_001", device.getId());
        assertEquals("apple", device.getPlatform());
        assertEquals("iphone", device.getDeviceType());
        assertEquals("active", device.getStatus());
        assertEquals("2024-01-01T00:00:00Z", device.getCreatedAt());
        assertEquals("2024-06-01T00:00:00Z", device.getUpdatedAt());
    }

    @Test
    public void cardWithMetadata() throws Exception {
        String json = "{"
            + "\"id\": \"0xc4rd1d\","
            + "\"metadata\": {"
            + "  \"department\": \"Engineering\","
            + "  \"floor\": 3"
            + "}"
            + "}";

        Models.Card card = mapper.readValue(json, Models.Card.class);

        assertNotNull(card.getMetadata());
        assertEquals("Engineering", card.getMetadata().get("department"));
        assertEquals(3, card.getMetadata().get("floor"));
    }

    @Test
    public void cardNullOptionalFields() throws Exception {
        String json = "{"
            + "\"id\": \"0xc4rd1d\","
            + "\"state\": \"active\""
            + "}";

        Models.Card card = mapper.readValue(json, Models.Card.class);

        assertEquals("0xc4rd1d", card.getId());
        assertEquals("active", card.getState());
        assertNull(card.getFullName());
        assertNull(card.getDevices());
        assertNull(card.getMetadata());
        assertNull(card.getInstallUrl());
    }

    @Test
    public void templateAllFields() throws Exception {
        String json = "{"
            + "\"id\": \"0xd3adb00b5\","
            + "\"name\": \"Employee NFC key\","
            + "\"platform\": \"apple\","
            + "\"use_case\": \"employee_badge\","
            + "\"protocol\": \"desfire\","
            + "\"allow_on_multiple_devices\": true,"
            + "\"watch_count\": 2,"
            + "\"iphone_count\": 3,"
            + "\"issued_keys_count\": 150,"
            + "\"active_keys_count\": 142,"
            + "\"created_at\": \"2024-01-01T00:00:00Z\","
            + "\"last_published_at\": \"2024-06-01T12:00:00Z\""
            + "}";

        Models.Template template = mapper.readValue(json, Models.Template.class);

        assertEquals("0xd3adb00b5", template.getId());
        assertEquals("Employee NFC key", template.getName());
        assertEquals("apple", template.getPlatform());
        assertEquals("employee_badge", template.getUseCase());
        assertEquals("desfire", template.getProtocol());
        assertTrue(template.isAllowOnMultipleDevices());
        assertEquals(2, template.getWatchCount());
        assertEquals(3, template.getIphoneCount());
        assertEquals(150, template.getIssuedKeysCount());
        assertEquals(142, template.getActiveKeysCount());
        assertEquals("2024-01-01T00:00:00Z", template.getCreatedAt());
        assertEquals("2024-06-01T12:00:00Z", template.getLastPublishedAt());
    }

    @Test
    public void templateDesign() throws Exception {
        String json = "{"
            + "\"background_color\": \"#FFFFFF\","
            + "\"label_color\": \"#000000\","
            + "\"label_secondary_color\": \"#333333\","
            + "\"background_image\": \"https://example.com/bg.png\","
            + "\"logo_image\": \"https://example.com/logo.png\","
            + "\"icon_image\": \"https://example.com/icon.png\""
            + "}";

        Models.TemplateDesign design = mapper.readValue(json, Models.TemplateDesign.class);

        assertEquals("#FFFFFF", design.getBackgroundColor());
        assertEquals("#000000", design.getLabelColor());
        assertEquals("#333333", design.getLabelSecondaryColor());
        assertEquals("https://example.com/bg.png", design.getBackgroundImage());
        assertEquals("https://example.com/logo.png", design.getLogoImage());
        assertEquals("https://example.com/icon.png", design.getIconImage());
    }

    @Test
    public void supportInfo() throws Exception {
        String json = "{"
            + "\"support_url\": \"https://help.example.com\","
            + "\"support_phone_number\": \"+1-555-123-4567\","
            + "\"support_email\": \"support@example.com\","
            + "\"privacy_policy_url\": \"https://example.com/privacy\","
            + "\"terms_and_conditions_url\": \"https://example.com/terms\""
            + "}";

        Models.SupportInfo info = mapper.readValue(json, Models.SupportInfo.class);

        assertEquals("https://help.example.com", info.getSupportUrl());
        assertEquals("+1-555-123-4567", info.getSupportPhoneNumber());
        assertEquals("support@example.com", info.getSupportEmail());
        assertEquals("https://example.com/privacy", info.getPrivacyPolicyUrl());
        assertEquals("https://example.com/terms", info.getTermsAndConditionsUrl());
    }

    @Test
    public void deviceStandalone() throws Exception {
        String json = "{"
            + "\"id\": \"dev_001\","
            + "\"platform\": \"google\","
            + "\"device_type\": \"phone\","
            + "\"status\": \"suspended\","
            + "\"created_at\": \"2024-03-15T10:30:00Z\","
            + "\"updated_at\": \"2024-07-20T14:00:00Z\""
            + "}";

        Models.Device device = mapper.readValue(json, Models.Device.class);

        assertEquals("dev_001", device.getId());
        assertEquals("google", device.getPlatform());
        assertEquals("phone", device.getDeviceType());
        assertEquals("suspended", device.getStatus());
        assertEquals("2024-03-15T10:30:00Z", device.getCreatedAt());
        assertEquals("2024-07-20T14:00:00Z", device.getUpdatedAt());
    }

    @Test
    public void unknownFieldsAreIgnored() throws Exception {
        String json = "{"
            + "\"id\": \"0xc4rd1d\","
            + "\"state\": \"active\","
            + "\"some_future_field\": \"should not break\""
            + "}";

        // Should not throw even with unknown fields
        Models.Card card = mapper.readValue(json, Models.Card.class);
        assertEquals("0xc4rd1d", card.getId());
    }
}
