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
        @JsonProperty("allow_on_multiple_devices")
        private boolean allowOnMultipleDevices;
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
        private String title;
        private java.util.Map<String, Object> metadata;
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
        private String title;
    }

    /**
     * Response model for an access card.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Card {
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
        @JsonProperty("install_url")
        private String installUrl;
        @JsonProperty("direct_install_url")
        private String directInstallUrl;
        private Object details;
        private List<Device> devices;
        private java.util.Map<String, Object> metadata;

        public String getUrl() {
            return installUrl;
        }
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
        @JsonProperty("background_color")
        private String backgroundColor;
        @JsonProperty("label_color")
        private String labelColor;
        @JsonProperty("label_secondary_color")
        private String labelSecondaryColor;
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
        private java.util.Map<String, Object> metadata;
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
        @JsonProperty("background_color")
        private String backgroundColor;
        @JsonProperty("label_color")
        private String labelColor;
        @JsonProperty("label_secondary_color")
        private String labelSecondaryColor;
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
     * Parameters for listing access cards.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeysParams {
        @JsonProperty("template_id")
        private String templateId;
        private String state;
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
        @JsonProperty("ip_address")
        private String ipAddress;
        @JsonProperty("user_agent")
        private String userAgent;
        private Object metadata;
    }

    /**
     * Response wrapper for event logs.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventLogResponse {
        private List<Event> events;
    }

    /**
     * Parameters for listing ledger items.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerItemsParams {
        private Integer page;
        @JsonProperty("per_page")
        private Integer perPage;
        @JsonProperty("start_date")
        private ZonedDateTime startDate;
        @JsonProperty("end_date")
        private ZonedDateTime endDate;
    }

    /**
     * Ledger item access pass info.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerAccessPass {
        @JsonProperty("ex_id")
        private String exId;
        @JsonProperty("pass_template")
        private LedgerPassTemplate passTemplate;
    }

    /**
     * Ledger item pass template info.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerPassTemplate {
        @JsonProperty("ex_id")
        private String exId;
    }

    /**
     * Ledger item model.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerItem {
        private String id;
        private String amount;
        private String kind;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("access_pass")
        private LedgerAccessPass accessPass;
    }

    /**
     * Response wrapper for ledger items.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerItemsResult {
        @JsonProperty("ledger_items")
        private List<LedgerItem> ledgerItems;
        private Pagination pagination;
    }

    /**
     * Pagination info.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        @JsonProperty("current_page")
        private int currentPage;
        @JsonProperty("total_pages")
        private int totalPages;
        @JsonProperty("total_count")
        private int totalCount;
        @JsonProperty("per_page")
        private int perPage;
    }

    /**
     * HID Organization model.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HIDOrg {
        private int id;
        private String name;
        private String slug;
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;
        private String phone;
        @JsonProperty("full_address")
        private String fullAddress;
        private String status;
        @JsonProperty("created_at")
        private String createdAt;
    }

    /**
     * Request model for creating a HID organization.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateHIDOrgParams {
        private String name;
        @JsonProperty("full_address")
        private String fullAddress;
        private String phone;
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;
    }

    /**
     * Request model for completing HID org registration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteHIDOrgParams {
        private String email;
        private String password;
    }

    /**
     * Response model for iOS In-App Provisioning preflight.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IosPreflightResponse {
        @JsonProperty("provisioningCredentialIdentifier")
        private String provisioningCredentialIdentifier;
        @JsonProperty("sharingInstanceIdentifier")
        private String sharingInstanceIdentifier;
        @JsonProperty("cardTemplateIdentifier")
        private String cardTemplateIdentifier;
        @JsonProperty("environmentIdentifier")
        private String environmentIdentifier;
    }
}