package com.organization.accessgrid;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Models used in the Access Grid SDK.
 */
public class Models {
    /**
     * Device associated with an access pass.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Device {
        private String id;
        private String platform;
        @JsonProperty("device_type")
        private String deviceType;
        private String status;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
    }

    /**
     * Request model for provisioning a new access card.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProvisionCardRequest {
        @JsonProperty("card_template_id")
        private String cardTemplateId;
        @JsonProperty("employee_id")
        private String employeeId;
        @JsonProperty("tag_id")
        private String tagId;
        @JsonProperty("full_name")
        private String fullName;
        private String email;
        @JsonProperty("phone_number")
        private String phoneNumber;
        private String classification;
        @JsonProperty("start_date")
        private String startDate;
        @JsonProperty("expiration_date")
        private String expirationDate;
        @JsonProperty("employee_photo")
        private String employeePhoto;
    }

    /**
     * Request model for updating an existing access card.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCardRequest {
        @JsonProperty("card_id")
        private String cardId;
        @JsonProperty("employee_id")
        private String employeeId;
        @JsonProperty("full_name")
        private String fullName;
        private String classification;
        @JsonProperty("expiration_date")
        private String expirationDate;
        @JsonProperty("employee_photo")
        private String employeePhoto;
    }

    /**
     * Response model for an access card.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Card {
        @JsonProperty("install_url")
        private String url;
        private String id;
        private String state;
        @JsonProperty("full_name")
        private String fullName;
        @JsonProperty("expiration_date")
        private String expirationDate;
        @JsonProperty("card_template_id")
        private String cardTemplateId;
        @JsonProperty("card_number")
        private String cardNumber;
        @JsonProperty("site_code")
        private String siteCode;
        @JsonProperty("file_data")
        private String fileData;
        @JsonProperty("direct_install_url")
        private String directInstallUrl;
        @JsonProperty("install_url")
        private String installUrl;
        private Object details;
        private List<Device> devices;
        private java.util.Map<String, Object> metadata;
    }

    /**
     * Request model for creating a new card template.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTemplateRequest {
        private String name;
        private String platform;
        @JsonProperty("use_case")
        private String useCase;
        private String protocol;
        @JsonProperty("allow_on_multiple_devices")
        private boolean allowOnMultipleDevices;
        @JsonProperty("watch_count")
        private int watchCount;
        @JsonProperty("iphone_count")
        private int iphoneCount;
        private TemplateDesign design;
        @JsonProperty("support_info")
        private SupportInfo supportInfo;
    }

    /**
     * Request model for updating an existing card template.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTemplateRequest {
        @JsonProperty("card_template_id")
        private String cardTemplateId;
        private String name;
        @JsonProperty("allow_on_multiple_devices")
        private boolean allowOnMultipleDevices;
        @JsonProperty("watch_count")
        private int watchCount;
        @JsonProperty("iphone_count")
        private int iphoneCount;
        @JsonProperty("support_info")
        private SupportInfo supportInfo;
    }

    /**
     * Template design configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateDesign {
        @JsonProperty("background_color")
        private String backgroundColor;
        @JsonProperty("label_color")
        private String labelColor;
        @JsonProperty("label_secondary_color")
        private String labelSecondaryColor;
        @JsonProperty("background_image")
        private String backgroundImage;
        @JsonProperty("logo_image")
        private String logoImage;
        @JsonProperty("icon_image")
        private String iconImage;
    }

    /**
     * Support information for a template.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportInfo {
        @JsonProperty("support_url")
        private String supportUrl;
        @JsonProperty("support_phone_number")
        private String supportPhoneNumber;
        @JsonProperty("support_email")
        private String supportEmail;
        @JsonProperty("privacy_policy_url")
        private String privacyPolicyUrl;
        @JsonProperty("terms_and_conditions_url")
        private String termsAndConditionsUrl;
    }

    /**
     * Template response model.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Template {
        private String id;
        private String name;
        private String platform;
        @JsonProperty("use_case")
        private String useCase;
        private String protocol;
        @JsonProperty("allow_on_multiple_devices")
        private boolean allowOnMultipleDevices;
        @JsonProperty("watch_count")
        private int watchCount;
        @JsonProperty("iphone_count")
        private int iphoneCount;
        @JsonProperty("issued_keys_count")
        private int issuedKeysCount;
        @JsonProperty("active_keys_count")
        private int activeKeysCount;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("last_published_at")
        private String lastPublishedAt;
    }

    /**
     * Event log filters for querying event logs.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventLogFilters {
        private String device;
        @JsonProperty("start_date")
        private ZonedDateTime startDate;
        @JsonProperty("end_date")
        private ZonedDateTime endDate;
        @JsonProperty("event_type")
        private String eventType;
    }

    /**
     * Event log entry model.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Event {
        private String type;
        private ZonedDateTime timestamp;
        @JsonProperty("user_id")
        private String userId;
        private String ipAddress;
        @JsonProperty("user_agent")
        private String userAgent;
        private Object metadata;
    }
}