# Access Grid Java SDK

## Overview

The Access Grid SDK is a Java library for seamlessly interacting with the Access Grid API. It provides a simple, type-safe interface for managing access cards, templates, and performing enterprise-level operations.

## Features

- 🔐 Secure API Authentication
- 📇 Access Card Management
- 🏢 Enterprise Template Creation and Management
- 🚀 Easy-to-Use Fluent API
- ☕ Full Java 11+ Support

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.accessgrid</groupId>
    <artifactId>access-grid-sdk</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```groovy
implementation 'com.accessgrid:access-grid-sdk:1.1.0'
```

## Quick Start

### Initializing the Client

```java
AccessGridClient client = new AccessGridClient(
    System.getenv("ACCESS_GRID_ACCOUNT_ID"), 
    System.getenv("ACCESS_GRID_SECRET_KEY")
);
```

### Provisioning an Access Card

```java
ProvisionCardRequest request = ProvisionCardRequest.builder()
    .cardTemplateId("template-123")
    .employeeId("emp-456")
    .fullName("John Doe")
    .email("john.doe@company.com")
    .classification("full_time")
    .build();

Card card = client.accessCards().provision(request);
System.out.println("Card Installed: " + card.getUrl());
```

### UnifiedAccessPass (Template Pairs)

When provisioning to a template pair (Apple + Android), you'll receive a `UnifiedAccessPass` containing both cards:

```java
ProvisionCardRequest request = ProvisionCardRequest.builder()
    .cardTemplateId("99b18646f12")  // Template pair ID
    .fullName("Jane Smith")
    .email("jane.smith@company.com")
    .build();

Union result = client.accessCards().provision(request);

if (result instanceof UnifiedAccessPass) {
    UnifiedAccessPass pass = (UnifiedAccessPass) result;
    System.out.println("Unified Pass ID: " + pass.getId());
    System.out.println("Install URL: " + pass.getUrl());
    System.out.println("Cards count: " + pass.getDetails().size());

    // Access individual cards
    for (Card card : pass.getDetails()) {
        System.out.println("Card ID: " + card.getId());
        System.out.println("Template: " + card.getCardTemplateId());
    }
}
```

**Important:** `get()` always returns `Card`, never `UnifiedAccessPass`. To retrieve individual cards from a pair, use the card IDs from the `details` array:

```java
Card appleCard = client.accessCards().get(pass.getDetails().get(0).getId());
Card androidCard = client.accessCards().get(pass.getDetails().get(1).getId());
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

### Creating a Card Template

```java
CreateTemplateRequest templateRequest = CreateTemplateRequest.builder()
    .name("Employee Access Card")
    .platform("apple")
    .useCase("employee_badge")
    .allowOnMultipleDevices(true)
    .build();

Template template = client.console().createTemplate(templateRequest);
```

## Error Handling

The SDK uses a custom `AccessGridException` for error management:

```java
try {
    client.accessCards().provision(request);
} catch (AccessGridException e) {
    // Handle API-specific errors
    System.err.println("Error: " + e.getMessage());
}
```

## Configuration

### Environment Variables

- `ACCESS_GRID_ACCOUNT_ID`: Your Access Grid account identifier
- `ACCESS_GRID_SECRET_KEY`: Your API secret key

### Logging

The SDK uses SLF4J for logging. Configure your preferred logging implementation.

## Advanced Usage

### Managing Card States

```java
// Suspend a card
client.accessCards().suspend("card-123");

// Resume a card
client.accessCards().resume("card-123");

// Unlink a card
client.accessCards().unlink("card-123");
```

## Dependencies

- Java 11+
- Jackson for JSON processing
- SLF4J for logging

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

[Specify your license, e.g., MIT, Apache 2.0]

## Support

For issues or questions, please open a GitHub issue or contact support@yourcompany.com.
