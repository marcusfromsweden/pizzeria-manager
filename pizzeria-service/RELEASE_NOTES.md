# Release Notes — Backend Refactoring (2026-02-13)

## Overview

Major codebase refactoring aligning the pizzeria-service with professional Spring Boot patterns.
This release reduces boilerplate, improves API documentation, centralizes cross-cutting concerns,
and strengthens the test infrastructure. Net result: ~800 fewer lines of code with better
maintainability and consistency.

## Highlights

### Lombok Boilerplate Reduction

- `@RequiredArgsConstructor` replaces hand-written constructors across ~26 classes (controllers,
  services, repository adapters)
- `@Getter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` on 8 R2DBC entity classes
- Net ~800 line reduction across the codebase

### OpenAPI Documentation

- `@Operation` and `@ApiResponse` annotations added to all 12 controllers
- `@CommonApiResponses` meta-annotation for shared error responses (400, 401, 403, 404, 500)
- Swagger UI now shows descriptions for every endpoint

### Centralized DTO Mapping

- New `RestDomainConverter` service consolidates all DTO-to-domain and domain-to-DTO mappings
- Replaces scattered static `toResponse()`, `toDomain()`, and `toEntity()` methods

### Dedicated Validators

- `UserValidator` and `OrderValidator` extracted from service classes
- Violation-collection pattern: validates all fields and returns all errors at once
- `DomainValidationException` enhanced to hold a list of violations
- `RestErrorHandler` renders multi-violation error responses

### Self-Documenting Exceptions

- `@ResponseStatus` added to all 4 custom exception classes:
  - 422 Unprocessable Entity
  - 404 Not Found
  - 401 Unauthorized
  - 403 Forbidden

### Audit Trail

- `created_by` and `updated_by` columns added to 5 tables: `users`, `feedback`, `pizza_scores`,
  `orders`, `order_items`
- Liquibase migration included
- Domain models updated with new audit fields

### Test Infrastructure

- `PostgresContainerSupport` base class for Testcontainers-based integration tests
- `@PizzeriaIntegrationTest` composite annotation for integration test configuration
- New test classes: `RestDomainConverterTest`, `UserValidatorTest`, `OrderValidatorTest`
- All affected unit and integration tests updated

## Stats

- 68 files changed
- 2,045 insertions / 1,818 deletions
- 10 new files
