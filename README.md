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
    <groupId>com.organization</groupId>
    <artifactId>access-grid-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```groovy
implementation 'com.organization:access-grid-sdk:1.0.0'
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
