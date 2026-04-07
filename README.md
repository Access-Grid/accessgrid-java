# Access Grid Java SDK

## Overview

The Access Grid SDK is a Java library for seamlessly interacting with the Access Grid API. It provides a simple, type-safe interface for managing access cards, templates, and performing enterprise-level operations.

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.accessgrid</groupId>
    <artifactId>access-grid-sdk</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```groovy
implementation 'com.accessgrid:access-grid-sdk:1.2.0'
```

## Quick Start

### Initializing the Client

```java
AccessGridClient client = new AccessGridClient(
    System.getenv("ACCOUNT_ID"),
    System.getenv("SECRET_KEY")
);
```

### Issuing an Access Card

```java
ProvisionCardRequest request = ProvisionCardRequest.builder()
    .cardTemplateId("0xd3adb00b5")
    .employeeId("123456789")
    .tagId("DDEADB33FB00B5")
    .allowOnMultipleDevices(true)
    .fullName("Employee name")
    .email("employee@yourwebsite.com")
    .phoneNumber("+19547212241")
    .classification("full_time")
    .startDate(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
    .expirationDate("2026-04-01T00:00:00.000Z")
    .employeePhoto("[image_in_base64_encoded_format]")
    .title("Engineering Manager")
    .metadata(Map.of(
        "department", "engineering",
        "badge_type", "contractor"
    ))
    .build();

Card card = client.accessCards().provision(request);
System.out.printf("Install URL: %s%n", card.getUrl());
```

### Getting an Access Card

```java
Card card = client.accessCards().get("0xc4rd1d");

System.out.println("Card ID: " + card.getId());
System.out.println("State: " + card.getState());
System.out.println("Full Name: " + card.getFullName());
System.out.println("Install URL: " + card.getInstallUrl());
System.out.println("Expiration Date: " + card.getExpirationDate());
System.out.println("Card Number: " + card.getCardNumber());
System.out.println("Site Code: " + card.getSiteCode());
System.out.println("Devices: " + card.getDevices().size());
System.out.println("Metadata: " + card.getMetadata());
```

### Updating an Access Card

```java
UpdateCardRequest request = UpdateCardRequest.builder()
    .cardId("0xc4rd1d")
    .employeeId("987654321")
    .fullName("Updated Employee Name")
    .classification("contractor")
    .expirationDate(ZonedDateTime.now().plusMonths(3).format(DateTimeFormatter.ISO_INSTANT))
    .employeePhoto("[image_in_base64_encoded_format]")
    .title("Senior Developer")
    .build();

client.accessCards().update(request);
```

### Listing Access Cards

```java
// Get filtered keys by template
ListKeysParams templateFilter = ListKeysParams.builder()
    .templateId("0xd3adb00b5")
    .build();
List<Card> templateKeys = client.accessCards().list(templateFilter);

// Get filtered keys by state
ListKeysParams stateFilter = ListKeysParams.builder()
    .state("active")
    .build();
List<Card> activeKeys = client.accessCards().list(stateFilter);
```

### Managing Card States

```java
// Suspend a card
client.accessCards().suspend("0xc4rd1d");

// Resume a card
client.accessCards().resume("0xc4rd1d");

// Unlink a card
client.accessCards().unlink("0xc4rd1d");

// Delete a card
client.accessCards().delete("0xc4rd1d");
```

## Console (Enterprise Features)

### Creating a Card Template

```java
CreateTemplateRequest request = CreateTemplateRequest.builder()
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
    .supportUrl("https://help.yourcompany.com")
    .supportPhoneNumber("+1-555-123-4567")
    .supportEmail("support@yourcompany.com")
    .privacyPolicyUrl("https://yourcompany.com/privacy")
    .termsAndConditionsUrl("https://yourcompany.com/terms")
    .metadata(Map.of(
        "version", "2.1",
        "approval_status", "approved"
    ))
    .build();

Template template = client.console().createTemplate(request);
```

### Updating a Card Template

```java
UpdateTemplateRequest request = UpdateTemplateRequest.builder()
    .cardTemplateId("0xd3adb00b5")
    .name("Updated Employee Access Pass")
    .allowOnMultipleDevices(true)
    .watchCount(2)
    .iphoneCount(3)
    .backgroundColor("#FFFFFF")
    .labelColor("#000000")
    .labelSecondaryColor("#333333")
    .supportUrl("https://help.yourcompany.com")
    .supportPhoneNumber("+1-555-123-4567")
    .supportEmail("support@yourcompany.com")
    .privacyPolicyUrl("https://yourcompany.com/privacy")
    .termsAndConditionsUrl("https://yourcompany.com/terms")
    .build();

Template template = client.console().updateTemplate(request);
```

### Reading a Card Template

```java
Template template = client.console().readTemplate("0xd3adb00b5");

System.out.printf("Template ID: %s%n", template.getId());
System.out.printf("Name: %s%n", template.getName());
System.out.printf("Platform: %s%n", template.getPlatform());
System.out.printf("Protocol: %s%n", template.getProtocol());
System.out.printf("Multi-device: %b%n", template.isAllowOnMultipleDevices());
```

### Event Logs

```java
EventLogFilters filters = EventLogFilters.builder()
    .device("mobile")
    .startDate(ZonedDateTime.now().minusDays(30))
    .endDate(ZonedDateTime.now())
    .eventType("install")
    .build();

List<Event> events = client.console().eventLog("0xd3adb00b5", filters);

for (Event event : events) {
    System.out.printf("Event: %s at %s by %s%n",
        event.getType(),
        event.getTimestamp(),
        event.getUserId());
}
```

### Ledger Items

```java
LedgerItemsParams params = LedgerItemsParams.builder()
    .page(1)
    .perPage(50)
    .startDate(ZonedDateTime.now().minusDays(30))
    .endDate(ZonedDateTime.now())
    .build();

LedgerItemsResult result = client.console().ledgerItems(params);

for (LedgerItem item : result.getLedgerItems()) {
    System.out.printf("Amount: %s, Kind: %s, Date: %s%n",
        item.getAmount(),
        item.getKind(),
        item.getCreatedAt());
}
```

### HID Organizations

```java
// Create HID org
CreateHIDOrgParams params = CreateHIDOrgParams.builder()
    .name("My Org")
    .fullAddress("1 Main St, NY NY")
    .phone("+1-555-0000")
    .firstName("Ada")
    .lastName("Lovelace")
    .build();

HIDOrg org = client.console().hid().orgs().create(params);

// List all HID orgs
List<HIDOrg> orgs = client.console().hid().orgs().list();

// Complete HID org registration
CompleteHIDOrgParams completeParams = CompleteHIDOrgParams.builder()
    .email("admin@example.com")
    .password("hid-password-123")
    .build();

HIDOrg result = client.console().hid().orgs().activate(completeParams);
```

## Error Handling

```java
try {
    client.accessCards().provision(request);
} catch (AccessGridException e) {
    System.err.println("Error: " + e.getMessage());
}
```

## Dependencies

- Java 11+
- Jackson for JSON processing
- Lombok for reducing boilerplate
- SLF4J for logging

## Feature Matrix

| Feature | Supported |
|---|:---:|
| POST /v1/key-cards (issue) | Y |
| GET /v1/key-cards/{id} | Y |
| PATCH /v1/key-cards/{id} | Y |
| GET /v1/key-cards (list) | Y |
| POST .../suspend | Y |
| POST .../resume | Y |
| POST .../unlink | Y |
| POST .../delete | Y |
| POST /v1/console/card-templates | Y |
| PUT /v1/console/card-templates/{id} | Y |
| GET /v1/console/card-templates/{id} | Y |
| GET .../logs | Y |
| GET /v1/console/ledger-items | Y |
| POST /v1/console/ios-preflight | Y |
| HID orgs (create/activate/list) | Y |
