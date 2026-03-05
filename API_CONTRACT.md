# Pizzeria Service - Frontend API Contract

This document defines the complete API contract for the pizzeria-service backend, optimized for React + TypeScript frontend consumption.

---

**## Table of Contents

1. [Global API Conventions](#global-api-conventions)
2. [Authentication and Authorization](#authentication-and-authorization)
3. [Multi-Tenant Support](#multi-tenant-support)
4. [Domain Models](#domain-models)
5. [API Endpo**ints](#api-endpoints)
6. [Error Handling](#error-handling)
7. [Pagination, Filtering and Sorting](#pagination-filtering-and-sorting)
8. [Frontend-Focused Guarantees](#frontend-focused-guarantees)
9. [Example Payloads](#example-payloads)
10. [Versioning and Backward Compatibility](#versioning-and-backward-compatibility)
11. [Explicit Non-Goals](#explicit-non-goals)

---

## Global API Conventions

### Base API

| Property | Value |
|----------|-------|
| **Base URL** | `http://localhost:9900` (development) |
| **API Version Prefix** | `/api/v1` |
| **JSON Naming Convention** | `camelCase` |
| **Content-Type** | `application/json` |
| **Accept** | `application/json` |
| **Character Encoding** | `UTF-8` |

### Request Headers

```
Content-Type: application/json
Accept: application/json
Accept-Language: en (or sv for Swedish)
Authorization: Bearer <token>  (for authenticated endpoints)
```

### Response Headers

```
Content-Type: application/json
```

---

## Authentication and Authorization

### Authentication Mechanism

| Property | Value |
|----------|-------|
| **Type** | Custom Bearer Token (opaque) |
| **Header** | `Authorization: Bearer <token>` |
| **Token Format** | Base64 URL-encoded random string (32 bytes) |
| **Token Storage** | In-memory on server (not JWT) |

### Authentication Flow

```
1. REGISTER  → POST /api/v1/pizzerias/{pizzeriaCode}/users/register
              ← Returns: userId, verificationToken

2. VERIFY    → POST /api/v1/pizzerias/{pizzeriaCode}/users/verify-email
              ← Returns: 204 No Content (email now verified)

3. LOGIN     → POST /api/v1/pizzerias/{pizzeriaCode}/users/login
              ← Returns: accessToken

4. USE TOKEN → Include header: Authorization: Bearer <accessToken>

5. LOGOUT    → POST /api/v1/users/logout
              ← Returns: 204 No Content (token invalidated)
```

### Token Lifecycle

| Property | Value |
|----------|-------|
| **Expiration** | None (tokens do not expire automatically) |
| **Invalidation** | Manual logout only |
| **Refresh Mechanism** | None (re-login required after logout) |
| **Persistence** | Not persisted (lost on server restart) |

### User Roles

The current implementation has a **single role**: authenticated user (customer).

| Role | Description |
|------|-------------|
| `CUSTOMER` | Registered, email-verified user |

**Note:** No admin, kitchen, or super-admin roles are currently implemented.

### Access Control Per Endpoint

| Endpoint Pattern | Access |
|------------------|--------|
| `POST /api/v1/pizzerias/{code}/users/register` | Public |
| `POST /api/v1/pizzerias/{code}/users/verify-email` | Public |
| `POST /api/v1/pizzerias/{code}/users/login` | Public |
| `GET /api/v1/pizzerias/{code}/menu` | Public |
| `GET /api/v1/pizzerias/{code}/pizzas/**` | Public |
| `GET /api/v1/pizzerias/{code}/ingredients/**` | Public |
| `GET /swagger-ui/**` | Public |
| `GET /v3/api-docs/**` | Public |
| `GET /actuator/health` | Public |
| All other `/api/v1/**` endpoints | Authenticated |

---

## Multi-Tenant Support

### Tenant Identification

| Context | Identification Method |
|---------|----------------------|
| **Public endpoints** | `pizzeriaCode` path parameter in URL |
| **Authenticated endpoints** | `pizzeriaId` extracted from Bearer token |

### Tenant Resolution Flow

**Public Endpoints:**
```
GET /api/v1/pizzerias/kingspizza/menu
                      ^^^^^^^^^^
                      pizzeriaCode resolved to pizzeriaId via database lookup
```

**Authenticated Endpoints:**
```
GET /api/v1/users/me
    Authorization: Bearer <token>
                          ^^^^^
                          Token contains userId + pizzeriaId
```

### Frontend Requirements

| Scenario | Frontend Action |
|----------|-----------------|
| Registration, Login, Menu browsing | Include `pizzeriaCode` in URL path |
| Profile, Preferences, Ratings | Only include Bearer token (no pizzeriaCode needed) |

### Data Isolation

All data is isolated by `pizzeriaId`. Users from one pizzeria cannot access data from another pizzeria.

---

## Domain Models

### Enums

#### DietType
```typescript
type DietType = "VEGAN" | "VEGETARIAN" | "CARNIVORE" | "NONE";
```

#### PizzaType
```typescript
type PizzaType = "TEMPLATE" | "CUSTOM";
```

#### FeedbackType
```typescript
type FeedbackType = "SERVICE" | "PIZZA";
```

#### FeedbackStatus (internal)
```typescript
type FeedbackStatus = "OPEN" | "IN_REVIEW" | "RESOLVED" | "DISMISSED";
```

#### UserStatus (internal)
```typescript
type UserStatus = "ACTIVE" | "SUSPENDED" | "DELETED";
```

#### IngredientAvailability
```typescript
type IngredientAvailability = "IN_STOCK" | "LIMITED" | "OUT_OF_STOCK";
```

---

### Entity: Pizzeria (Tenant)

**Purpose:** Represents a pizzeria tenant in the multi-tenant system.

```json
{
  "id": "UUID",
  "code": "string (unique identifier, URL-safe)",
  "name": "string",
  "active": "boolean"
}
```

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | `UUID` | Yes | No | Unique identifier |
| `code` | `string` | Yes | No | URL-safe code (e.g., "kingspizza") |
| `name` | `string` | Yes | No | Display name |
| `active` | `boolean` | Yes | No | Whether pizzeria is active |

**Note:** Pizzeria is resolved from URL path, not directly returned to frontend.

---

### Entity: User

**Purpose:** Registered customer account.

```json
{
  "id": "UUID",
  "name": "string",
  "email": "string",
  "emailVerified": "boolean",
  "preferredDiet": "DietType",
  "preferredIngredientIds": ["UUID"],
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | `UUID` | Yes | No | Unique identifier |
| `name` | `string` | Yes | No | User's full name (max 100 chars) |
| `email` | `string` | Yes | No | Email address (max 255 chars) |
| `emailVerified` | `boolean` | Yes | No | Email verification status |
| `preferredDiet` | `DietType` | Yes | No | Dietary preference (default: `NONE`) |
| `preferredIngredientIds` | `UUID[]` | Yes | No | List of preferred ingredient IDs |
| `createdAt` | `string` | Yes | No | ISO-8601 UTC timestamp |
| `updatedAt` | `string` | Yes | No | ISO-8601 UTC timestamp |

---

### Entity: MenuSection

**Purpose:** Category/section within the menu (e.g., "Pizzas", "Salads").

```json
{
  "id": "UUID",
  "code": "string",
  "translationKey": "string",
  "sortOrder": "integer",
  "items": ["MenuItemResponse"]
}
```

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | `UUID` | Yes | No | Unique identifier |
| `code` | `string` | Yes | No | Section code (e.g., "pizza", "salad") |
| `translationKey` | `string` | Yes | No | i18n key for section name |
| `sortOrder` | `integer` | Yes | No | Display order (ascending) |
| `items` | `MenuItemResponse[]` | Yes | No | Menu items in this section |

---

### Entity: MenuItem (Product/Pizza)

**Purpose:** Individual menu item (pizza, salad, pasta, etc.).

```json
{
  "id": "UUID",
  "sectionId": "UUID",
  "dishNumber": "integer",
  "nameKey": "string",
  "descriptionKey": "string",
  "priceInSek": "decimal",
  "familySizePriceInSek": "decimal",
  "ingredients": ["MenuIngredientResponse"],
  "sortOrder": "integer"
}
```

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | `UUID` | Yes | No | Unique identifier |
| `sectionId` | `UUID` | Yes | No | Parent section ID |
| `dishNumber` | `integer` | Yes | No | Menu item number (e.g., 1, 2, 3) |
| `nameKey` | `string` | Yes | No | i18n key for item name |
| `descriptionKey` | `string` | Yes | No | i18n key for description |
| `priceInSek` | `decimal` | Yes | No | Regular size price in SEK |
| `familySizePriceInSek` | `decimal` | Yes | No | Family size price in SEK |
| `ingredients` | `MenuIngredientResponse[]` | Yes | No | List of ingredients |
| `sortOrder` | `integer` | Yes | No | Display order within section |

---

### Entity: MenuIngredient

**Purpose:** Ingredient with dietary and allergen information.

```json
{
  "id": "UUID",
  "ingredientKey": "string",
  "dietaryType": "string",
  "allergenTags": ["string"],
  "spiceLevel": "integer"
}
```

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | `UUID` | Yes | No | Unique identifier |
| `ingredientKey` | `string` | Yes | No | i18n key for ingredient name |
| `dietaryType` | `string` | Yes | No | "VEGAN", "VEGETARIAN", or "CARNIVORE" |
| `allergenTags` | `string[]` | Yes | No | Allergen identifiers (e.g., ["gluten", "dairy"]) |
| `spiceLevel` | `integer` | Yes | No | Spiciness level (0-5) |

---

### Entity: PizzaCustomisation (Modifier/Extra)

**Purpose:** Available pizza modifications and extras.

```json
{
  "id": "UUID",
  "nameKey": "string",
  "priceInSek": "decimal",
  "familySizePriceInSek": "decimal",
  "sortOrder": "integer"
}
```

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | `UUID` | Yes | No | Unique identifier |
| `nameKey` | `string` | Yes | No | i18n key for customisation name |
| `priceInSek` | `decimal` | Yes | No | Additional cost for regular size |
| `familySizePriceInSek` | `decimal` | Yes | No | Additional cost for family size |
| `sortOrder` | `integer` | Yes | No | Display order |

---

### Entity: PizzaScore (Rating)

**Purpose:** User rating for a pizza.

```json
{
  "id": "UUID",
  "userId": "UUID",
  "pizzaId": "UUID",
  "pizzaType": "PizzaType",
  "score": "integer",
  "comment": "string | null",
  "createdAt": "ISO-8601 timestamp"
}
```

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | `UUID` | Yes | No | Unique identifier |
| `userId` | `UUID` | Yes | No | User who created the rating |
| `pizzaId` | `UUID` | Yes | No | Rated pizza ID |
| `pizzaType` | `PizzaType` | Yes | No | `TEMPLATE` or `CUSTOM` |
| `score` | `integer` | Yes | No | Rating (1-5) |
| `comment` | `string` | Yes | Yes | Optional comment |
| `createdAt` | `string` | Yes | No | ISO-8601 UTC timestamp |

---

### Entity: Feedback

**Purpose:** User feedback about the service.

```json
{
  "id": "UUID",
  "userId": "UUID",
  "type": "string",
  "message": "string",
  "rating": "integer | null",
  "createdAt": "ISO-8601 timestamp"
}
```

| Field | Type | Required | Nullable | Description |
|-------|------|----------|----------|-------------|
| `id` | `UUID` | Yes | No | Unique identifier |
| `userId` | `UUID` | Yes | No | User who submitted feedback |
| `type` | `string` | Yes | No | Feedback type ("SERVICE") |
| `message` | `string` | Yes | No | Feedback message |
| `rating` | `integer` | Yes | Yes | Optional rating (1-5) |
| `createdAt` | `string` | Yes | No | ISO-8601 UTC timestamp |

---

## API Endpoints

### User Registration

#### POST /api/v1/pizzerias/{pizzeriaCode}/users/register

**Purpose:** Register a new user account.

**Authentication:** None (public)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pizzeriaCode` | `string` | Yes | Pizzeria identifier |

**Request Body:**

```json
{
  "name": "string",
  "email": "string",
  "password": "string"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | `string` | Yes | Max 100 characters, not blank |
| `email` | `string` | Yes | Valid email format, max 255 characters |
| `password` | `string` | Yes | Min 8 characters, max 100 characters |

**Success Response:**

| Status | Body |
|--------|------|
| `201 Created` | `UserRegisterResponse` |

```json
{
  "userId": "UUID",
  "emailVerified": false,
  "verificationToken": "string"
}
```

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `404 Not Found` | `RESOURCE_NOT_FOUND` | Pizzeria code not found |
| `422 Unprocessable Entity` | `INVALID_ARGUMENT` | Email already registered |
| `422 Unprocessable Entity` | `INVALID_ARGUMENT` | Validation failed |

**Behavioral Guarantees:**
- Idempotent: No (creates new user each time)
- Side effects: User record created in database
- Password is hashed with bcrypt before storage

---

### Email Verification

#### POST /api/v1/pizzerias/{pizzeriaCode}/users/verify-email

**Purpose:** Verify user's email address using token from registration.

**Authentication:** None (public)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pizzeriaCode` | `string` | Yes | Pizzeria identifier |

**Request Body:**

```json
{
  "token": "string"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `token` | `string` | Yes | Not blank |

**Success Response:**

| Status | Body |
|--------|------|
| `204 No Content` | Empty |

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `404 Not Found` | `RESOURCE_NOT_FOUND` | Invalid or expired token |
| `404 Not Found` | `RESOURCE_NOT_FOUND` | Pizzeria not found |

**Behavioral Guarantees:**
- Idempotent: Yes (verifying already-verified user returns 204)
- Token is single-use (consumed after successful verification)

---

### User Login

#### POST /api/v1/pizzerias/{pizzeriaCode}/users/login

**Purpose:** Authenticate user and obtain access token.

**Authentication:** None (public)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pizzeriaCode` | `string` | Yes | Pizzeria identifier |

**Request Body:**

```json
{
  "email": "string",
  "password": "string"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `email` | `string` | Yes | Valid email format |
| `password` | `string` | Yes | Not blank |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `UserLoginResponse` |

```json
{
  "accessToken": "string"
}
```

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `401 Unauthorized` | `UNAUTHORIZED` | Invalid email or password |
| `401 Unauthorized` | `UNAUTHORIZED` | Email not verified |
| `404 Not Found` | `RESOURCE_NOT_FOUND` | Pizzeria not found |

**Behavioral Guarantees:**
- Each login generates a new token
- Previous tokens remain valid (no single-session enforcement)

---

### User Logout

#### POST /api/v1/users/logout

**Purpose:** Invalidate the current access token.

**Authentication:** Required (Bearer token)

**Request Headers:**

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | `Bearer <token>` |

**Request Body:** None

**Success Response:**

| Status | Body |
|--------|------|
| `204 No Content` | Empty |

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `401 Unauthorized` | `UNAUTHORIZED` | Invalid or missing token |

**Behavioral Guarantees:**
- Idempotent: Yes (logging out with invalid token returns 401)
- Token is immediately invalidated

---

### Get User Profile

#### GET /api/v1/users/me

**Purpose:** Retrieve current user's profile.

**Authentication:** Required

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `UserProfileResponse` |

```json
{
  "id": "UUID",
  "name": "string",
  "email": "string",
  "emailVerified": true,
  "preferredDiet": "DietType",
  "preferredIngredientIds": ["UUID"],
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `401 Unauthorized` | `UNAUTHORIZED` | Invalid or missing token |
| `404 Not Found` | `RESOURCE_NOT_FOUND` | User not found (edge case) |

---

### Update User Profile

#### PATCH /api/v1/users/me

**Purpose:** Update current user's profile.

**Authentication:** Required

**Request Body:**

```json
{
  "name": "string | null",
  "phone": "string | null"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | `string` | No | Max 100 characters |
| `phone` | `string` | No | Max 30 characters |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `UserProfileResponse` |

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `401 Unauthorized` | `UNAUTHORIZED` | Invalid or missing token |
| `422 Unprocessable Entity` | `INVALID_ARGUMENT` | Validation failed |

**Behavioral Guarantees:**
- Partial update: Only provided fields are updated
- Null values: Ignored (field not updated)
- `updatedAt` timestamp is updated on successful modification

---

### Delete User Account

#### DELETE /api/v1/users/me

**Purpose:** Delete current user's account.

**Authentication:** Required

**Success Response:**

| Status | Body |
|--------|------|
| `204 No Content` | Empty |

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `401 Unauthorized` | `UNAUTHORIZED` | Invalid or missing token |

**Behavioral Guarantees:**
- Soft delete: User status changed to `DELETED`
- Associated tokens are invalidated
- User data may be retained for legal/audit purposes

---

### Get Diet Preferences

#### GET /api/v1/users/me/diet

**Purpose:** Retrieve current user's dietary preference.

**Authentication:** Required

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `DietPreferenceResponse` |

```json
{
  "diet": "DietType"
}
```

---

### Update Diet Preferences

#### PUT /api/v1/users/me/diet

**Purpose:** Update current user's dietary preference.

**Authentication:** Required

**Request Body:**

```json
{
  "diet": "DietType"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `diet` | `DietType` | Yes | One of: `VEGAN`, `VEGETARIAN`, `CARNIVORE`, `NONE` |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `DietPreferenceResponse` |

---

### Get Preferred Ingredients

#### GET /api/v1/users/me/preferences/ingredients/preferred

**Purpose:** List user's preferred ingredients.

**Authentication:** Required

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `IngredientIdResponse[]` |

```json
[
  { "ingredientId": "UUID" },
  { "ingredientId": "UUID" }
]
```

---

### Add Preferred Ingredient

#### POST /api/v1/users/me/preferences/ingredients/preferred

**Purpose:** Add an ingredient to user's preferred list.

**Authentication:** Required

**Request Body:**

```json
{
  "ingredientId": "UUID"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `ingredientId` | `UUID` | Yes | Valid UUID format |

**Success Response:**

| Status | Body |
|--------|------|
| `201 Created` | Empty |

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `404 Not Found` | `RESOURCE_NOT_FOUND` | Ingredient not found |
| `422 Unprocessable Entity` | `INVALID_ARGUMENT` | Ingredient already in preferences |

---

### Remove Preferred Ingredient

#### DELETE /api/v1/users/me/preferences/ingredients/preferred/{ingredientId}

**Purpose:** Remove an ingredient from user's preferred list.

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `ingredientId` | `UUID` | Yes | Ingredient ID to remove |

**Success Response:**

| Status | Body |
|--------|------|
| `204 No Content` | Empty |

**Behavioral Guarantees:**
- Idempotent: Yes (removing non-existent preference returns 204)

---

### Get Full Menu

#### GET /api/v1/pizzerias/{pizzeriaCode}/menu

**Purpose:** Retrieve complete menu with all sections, items, ingredients, and customisations.

**Authentication:** None (public)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pizzeriaCode` | `string` | Yes | Pizzeria identifier |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `MenuResponse` |

```json
{
  "sections": [
    {
      "id": "UUID",
      "code": "string",
      "translationKey": "string",
      "sortOrder": 0,
      "items": [
        {
          "id": "UUID",
          "sectionId": "UUID",
          "dishNumber": 1,
          "nameKey": "string",
          "descriptionKey": "string",
          "priceInSek": "decimal",
          "familySizePriceInSek": "decimal",
          "ingredients": [
            {
              "id": "UUID",
              "ingredientKey": "string",
              "dietaryType": "string",
              "allergenTags": ["string"],
              "spiceLevel": 0
            }
          ],
          "sortOrder": 0
        }
      ]
    }
  ],
  "pizzaCustomisations": [
    {
      "id": "UUID",
      "nameKey": "string",
      "priceInSek": "decimal",
      "familySizePriceInSek": "decimal",
      "sortOrder": 0
    }
  ]
}
```

**Behavioral Guarantees:**
- Sections are sorted by `sortOrder` ascending
- Items within sections are sorted by `sortOrder` ascending
- Ingredients within items are sorted by `sortOrder` ascending
- Customisations are sorted by `sortOrder` ascending

**Caching:**
- Menu data changes infrequently
- Safe to cache for 5-15 minutes on client

---

### List Pizzas

#### GET /api/v1/pizzerias/{pizzeriaCode}/pizzas

**Purpose:** List all pizzas with summary information.

**Authentication:** None (public)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pizzeriaCode` | `string` | Yes | Pizzeria identifier |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `PizzaSummaryResponse[]` |

```json
[
  {
    "id": "UUID",
    "dishNumber": 1,
    "nameKey": "string",
    "priceInSek": "decimal",
    "familySizePriceInSek": "decimal",
    "sortOrder": 0
  }
]
```

**Behavioral Guarantees:**
- Results sorted by `sortOrder` ascending
- No pagination (returns all pizzas)

---

### Get Pizza Details

#### GET /api/v1/pizzerias/{pizzeriaCode}/pizzas/{pizzaId}

**Purpose:** Get detailed information about a specific pizza.

**Authentication:** None (public)

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `pizzeriaCode` | `string` | Yes | Pizzeria identifier |
| `pizzaId` | `UUID` | Yes | Pizza ID |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `PizzaDetailResponse` |

```json
{
  "id": "UUID",
  "dishNumber": 1,
  "nameKey": "string",
  "descriptionKey": "string",
  "priceInSek": "decimal",
  "familySizePriceInSek": "decimal",
  "ingredientKeys": ["string"],
  "sortOrder": 0
}
```

**Error Responses:**

| Status | Error Code | When |
|--------|------------|------|
| `404 Not Found` | `RESOURCE_NOT_FOUND` | Pizza not found |

---

### Check Pizza Suitability

#### POST /api/v1/pizzas/suitability

**Purpose:** Check if a pizza (with customizations) suits the user's dietary preferences.

**Authentication:** Required

**Request Body:**

```json
{
  "pizzaId": "UUID",
  "additionalIngredientIds": ["UUID"] | null,
  "removedIngredientIds": ["UUID"] | null
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `pizzaId` | `UUID` | Yes | Valid UUID |
| `additionalIngredientIds` | `UUID[]` | No | List of ingredient IDs to add |
| `removedIngredientIds` | `UUID[]` | No | List of ingredient IDs to remove |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `PizzaSuitabilityResponse` |

```json
{
  "suitable": false,
  "violations": [
    "Pizza contains ham which is not suitable for vegetarian diet"
  ],
  "suggestions": [
    "Remove ham to make this pizza vegetarian-friendly"
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `suitable` | `boolean` | Whether pizza matches user's diet |
| `violations` | `string[]` | List of dietary violations |
| `suggestions` | `string[]` | Suggestions to make pizza suitable |

---

### Create Pizza Rating

#### POST /api/v1/pizza-scores

**Purpose:** Submit a rating for a pizza.

**Authentication:** Required

**Request Body:**

```json
{
  "pizzaId": "UUID",
  "pizzaType": "PizzaType",
  "score": 5,
  "comment": "string | null"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `pizzaId` | `UUID` | Yes | Valid UUID |
| `pizzaType` | `PizzaType` | Yes | `TEMPLATE` or `CUSTOM` |
| `score` | `integer` | Yes | Min 1, Max 5 |
| `comment` | `string` | No | Optional comment |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `PizzaScoreResponse` |

```json
{
  "id": "UUID",
  "userId": "UUID",
  "pizzaId": "UUID",
  "pizzaType": "TEMPLATE",
  "score": 5,
  "comment": "string | null",
  "createdAt": "ISO-8601 timestamp"
}
```

**Behavioral Guarantees:**
- Multiple ratings per user per pizza are allowed
- No update mechanism (create new rating instead)

---

### Get My Pizza Ratings

#### GET /api/v1/pizza-scores/me

**Purpose:** List all pizza ratings by current user.

**Authentication:** Required

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `PizzaScoreResponse[]` |

**Behavioral Guarantees:**
- Results sorted by `createdAt` descending (most recent first)
- No pagination (returns all ratings)

---

### Submit Service Feedback

#### POST /api/v1/feedback/service

**Purpose:** Submit feedback about the service.

**Authentication:** Required

**Request Body:**

```json
{
  "message": "string",
  "rating": 5,
  "category": "string | null"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `message` | `string` | Yes | Not blank |
| `rating` | `integer` | No | Min 1, Max 5 |
| `category` | `string` | No | Optional category |

**Success Response:**

| Status | Body |
|--------|------|
| `200 OK` | `FeedbackResponse` |

```json
{
  "id": "UUID",
  "userId": "UUID",
  "type": "SERVICE",
  "message": "string",
  "rating": 5,
  "createdAt": "ISO-8601 timestamp"
}
```

---

## Error Handling

### Error Response Format

All errors follow RFC 7807 Problem Details format:

```json
{
  "type": "about:blank",
  "title": "string",
  "status": 400,
  "detail": "string",
  "instance": null,
  "errorCode": "ERROR_CODE_ENUM",
  "timestamp": "ISO-8601 timestamp",
  "message": "string (localized, optional)"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `type` | `string` | Problem type URI (always "about:blank") |
| `title` | `string` | Short human-readable title |
| `status` | `integer` | HTTP status code |
| `detail` | `string` | Detailed error message |
| `errorCode` | `string` | Machine-readable error code |
| `timestamp` | `string` | ISO-8601 UTC timestamp |
| `message` | `string` | Localized message (for validation errors) |

### Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `INVALID_ARGUMENT` | `422` | Request validation failed |
| `RESOURCE_NOT_FOUND` | `404` | Requested resource not found |
| `UNAUTHORIZED` | `401` | Authentication required or failed |
| `DOWNSTREAM_ERROR` | `502` | External service error |
| `INTERNAL_ERROR` | `500` | Unexpected server error |

### Error Code Details

#### INVALID_ARGUMENT (422)

**When it occurs:**
- Request body validation fails (missing required fields, invalid formats)
- Business rule validation fails (email already registered, etc.)
- Invalid enum values provided

**Endpoints that may return this:**
- All POST/PUT/PATCH endpoints with request bodies

**Example:**
```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 422,
  "detail": "Email is already registered",
  "errorCode": "INVALID_ARGUMENT",
  "timestamp": "2024-01-15T10:30:00Z",
  "message": "Request data failed validation."
}
```

#### RESOURCE_NOT_FOUND (404)

**When it occurs:**
- Pizzeria code not found
- Pizza ID not found
- User ID not found
- Ingredient ID not found
- Verification token not found/expired

**Endpoints that may return this:**
- All endpoints with path parameters
- Email verification endpoint

**Example:**
```json
{
  "type": "about:blank",
  "title": "Resource not found",
  "status": 404,
  "detail": "Pizzeria with code 'invalid-code' not found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### UNAUTHORIZED (401)

**When it occurs:**
- Missing Authorization header
- Invalid or expired token
- Invalid login credentials
- Email not verified (during login)

**Endpoints that may return this:**
- All authenticated endpoints
- Login endpoint

**Example:**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid credentials",
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### INTERNAL_ERROR (500)

**When it occurs:**
- Unhandled server exceptions
- Database connection failures
- Unexpected runtime errors

**Endpoints that may return this:**
- Any endpoint

**Example:**
```json
{
  "type": "about:blank",
  "title": "Internal error",
  "status": 500,
  "detail": "An unexpected error occurred",
  "errorCode": "INTERNAL_ERROR",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Validation Error Details

For request validation errors, the `detail` field contains the specific validation failure:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 422,
  "detail": "password: size must be between 8 and 100",
  "errorCode": "INVALID_ARGUMENT",
  "timestamp": "2024-01-15T10:30:00Z",
  "message": "Request data failed validation."
}
```

---

## Pagination, Filtering and Sorting

### Current Implementation

**No pagination is implemented.** All list endpoints return complete results.

| Endpoint | Pagination | Sorting |
|----------|------------|---------|
| `GET /pizzerias/{code}/menu` | None | By `sortOrder` (ascending) |
| `GET /pizzerias/{code}/pizzas` | None | By `sortOrder` (ascending) |
| `GET /pizza-scores/me` | None | By `createdAt` (descending) |
| `GET /users/me/preferences/ingredients/preferred` | None | No guaranteed order |

### Filtering

No filtering parameters are currently supported on any endpoint.

### Sorting

Sorting is server-controlled and not configurable by the client:

| Entity | Default Sort |
|--------|--------------|
| Menu sections | `sortOrder` ASC |
| Menu items | `sortOrder` ASC |
| Ingredients | `sortOrder` ASC |
| Customisations | `sortOrder` ASC |
| Pizzas | `sortOrder` ASC |
| Pizza ratings | `createdAt` DESC |

---

## Frontend-Focused Guarantees

### Field Stability and Caching

| Question | Answer |
|----------|--------|
| Which fields are safe to cache? | Menu data, pizza listings, ingredient facts |
| Which fields may change frequently? | User profile, pizza ratings, feedback |
| Are IDs globally unique? | Yes, all IDs are UUIDs |
| Are timestamps always UTC? | Yes, all timestamps are ISO-8601 UTC |
| Can nullable fields change to non-null? | Yes, for optional fields like `phone`, `comment` |
| Are partial updates supported? | Yes, via PATCH on `/users/me` |

### Translation Keys

All `*Key` fields (e.g., `nameKey`, `translationKey`, `ingredientKey`) are i18n keys that should be resolved using a translation file or service.

**Available languages:** English (`en`), Swedish (`sv`)

**Key format examples:**
- `translation.key.disc.pizza.margarita` → "Margarita"
- `translation.key.ingredient.cheese` → "cheese"
- `translation.key.pizza.customisation.extra_cheese` → "Extra cheese"

### Price Format

All prices are in **Swedish Krona (SEK)** and represented as decimal strings with up to 2 decimal places:

```json
{
  "priceInSek": "95.00",
  "familySizePriceInSek": "175.00"
}
```

### UUID Format

All UUIDs follow RFC 4122 format:

```
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Example: `550e8400-e29b-41d4-a716-446655440000`

---

## Example Payloads

### User Registration

**Request:**
```http
POST /api/v1/pizzerias/kingspizza/users/register
Content-Type: application/json

{
  "name": "Maria Andersson",
  "email": "maria.andersson@example.com",
  "password": "securePassword123"
}
```

**Response (201 Created):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "emailVerified": false,
  "verificationToken": "dGVzdC12ZXJpZmljYXRpb24tdG9rZW4"
}
```

### User Login

**Request:**
```http
POST /api/v1/pizzerias/kingspizza/users/login
Content-Type: application/json

{
  "email": "maria.andersson@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo"
}
```

### User Profile

**Request:**
```http
GET /api/v1/users/me
Authorization: Bearer YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Maria Andersson",
  "email": "maria.andersson@example.com",
  "emailVerified": true,
  "preferredDiet": "VEGETARIAN",
  "preferredIngredientIds": [
    "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "b2c3d4e5-f6a7-8901-bcde-f12345678901"
  ],
  "createdAt": "2024-01-15T08:30:00Z",
  "updatedAt": "2024-01-15T10:45:00Z"
}
```

### Menu Response

**Request:**
```http
GET /api/v1/pizzerias/kingspizza/menu
```

**Response (200 OK):**
```json
{
  "sections": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "code": "pizza",
      "translationKey": "translation.key.section.pizza",
      "sortOrder": 1,
      "items": [
        {
          "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
          "sectionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "dishNumber": 1,
          "nameKey": "translation.key.disc.pizza.margarita",
          "descriptionKey": "translation.key.description.margarita",
          "priceInSek": "95.00",
          "familySizePriceInSek": "175.00",
          "ingredients": [
            {
              "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
              "ingredientKey": "translation.key.ingredient.cheese",
              "dietaryType": "VEGETARIAN",
              "allergenTags": ["dairy"],
              "spiceLevel": 0
            }
          ],
          "sortOrder": 1
        },
        {
          "id": "d4e5f6a7-b8c9-0123-def0-234567890123",
          "sectionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "dishNumber": 2,
          "nameKey": "translation.key.disc.pizza.hawaii",
          "descriptionKey": "translation.key.description.hawaii",
          "priceInSek": "105.00",
          "familySizePriceInSek": "195.00",
          "ingredients": [
            {
              "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
              "ingredientKey": "translation.key.ingredient.cheese",
              "dietaryType": "VEGETARIAN",
              "allergenTags": ["dairy"],
              "spiceLevel": 0
            },
            {
              "id": "e5f6a7b8-c9d0-1234-ef01-345678901234",
              "ingredientKey": "translation.key.ingredient.ham",
              "dietaryType": "CARNIVORE",
              "allergenTags": [],
              "spiceLevel": 0
            },
            {
              "id": "f6a7b8c9-d0e1-2345-f012-456789012345",
              "ingredientKey": "translation.key.ingredient.pineapple",
              "dietaryType": "VEGAN",
              "allergenTags": [],
              "spiceLevel": 0
            }
          ],
          "sortOrder": 2
        }
      ]
    }
  ],
  "pizzaCustomisations": [
    {
      "id": "g7h8i9j0-k1l2-3456-m789-567890123456",
      "nameKey": "translation.key.pizza.customisation.extra_cheese",
      "priceInSek": "15.00",
      "familySizePriceInSek": "25.00",
      "sortOrder": 1
    },
    {
      "id": "h8i9j0k1-l2m3-4567-n890-678901234567",
      "nameKey": "translation.key.pizza.customisation.gluten_free_pizza",
      "priceInSek": "20.00",
      "familySizePriceInSek": "35.00",
      "sortOrder": 2
    }
  ]
}
```

### Pizza Suitability Check

**Request:**
```http
POST /api/v1/pizzas/suitability
Authorization: Bearer YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo
Content-Type: application/json

{
  "pizzaId": "d4e5f6a7-b8c9-0123-def0-234567890123",
  "additionalIngredientIds": null,
  "removedIngredientIds": ["e5f6a7b8-c9d0-1234-ef01-345678901234"]
}
```

**Response (200 OK):**
```json
{
  "suitable": true,
  "violations": [],
  "suggestions": []
}
```

### Pizza Rating

**Request:**
```http
POST /api/v1/pizza-scores
Authorization: Bearer YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo
Content-Type: application/json

{
  "pizzaId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "pizzaType": "TEMPLATE",
  "score": 5,
  "comment": "Best Margarita in town!"
}
```

**Response (200 OK):**
```json
{
  "id": "i9j0k1l2-m3n4-5678-o901-789012345678",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "pizzaId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "pizzaType": "TEMPLATE",
  "score": 5,
  "comment": "Best Margarita in town!",
  "createdAt": "2024-01-15T12:30:00Z"
}
```

### Error Response

**Request (invalid token):**
```http
GET /api/v1/users/me
Authorization: Bearer invalid-token
```

**Response (401 Unauthorized):**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid or expired token",
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2024-01-15T12:35:00Z"
}
```

---

## Versioning and Backward Compatibility

### Current Version

**API Version:** `v1`
**Version Path:** `/api/v1/`

### Breaking Changes Policy

| Change Type | How Introduced |
|-------------|----------------|
| New endpoint | Added to existing version |
| New optional field | Added to existing response |
| New required field | New API version (v2) |
| Field removal | Deprecated first, then new version |
| Field type change | New API version |
| Endpoint removal | Deprecated first, then removed in new version |

### Deprecation Communication

Deprecated fields/endpoints will be communicated via:
1. `Deprecation` HTTP header
2. API documentation updates
3. Changelog announcements

### Version Detection

The frontend can detect API version from:
- URL path (`/api/v1/`, `/api/v2/`)
- OpenAPI spec at `/v3/api-docs`

---

## Explicit Non-Goals

The following behaviors and assumptions are **NOT guaranteed** and the frontend **MUST NOT** rely on them:

### Ordering Guarantees

1. **Preferred ingredients order:** No guaranteed order when listing preferred ingredients
2. **Multiple ratings order:** While currently sorted by `createdAt` DESC, this may change

### Field Stability

1. **Error message text:** Error detail messages may change; use `errorCode` for programmatic handling
2. **Translation key format:** Key patterns may evolve; do not parse key strings

### Authentication

1. **Token format:** Do not parse or decode tokens; treat as opaque strings
2. **Token persistence:** Tokens are in-memory only; do not assume persistence across server restarts
3. **Single session:** Multiple active tokens per user are allowed; do not assume single-session

### Multi-Tenancy

1. **Cross-tenant data:** Do not assume data isolation bugs won't occur; validate data ownership client-side
2. **Pizzeria discovery:** No endpoint to list available pizzerias; code must be known

### Data Constraints

1. **Unique ratings:** Users can submit multiple ratings for the same pizza; no uniqueness constraint
2. **Soft deletes:** Deleted users/data may still exist in database

### Performance

1. **Response time:** No SLA guarantees
2. **Rate limiting:** Not implemented; do not assume unlimited requests are safe

### Features Not Implemented

1. **Cart and Order management:** Not implemented
2. **Payment processing:** Not implemented
3. **Kitchen tickets/Preparation status:** Not implemented
4. **Real-time updates (WebSocket/SSE):** Not implemented
5. **Admin roles:** Not implemented
6. **Password reset:** Not implemented
7. **Token refresh:** Not implemented

---

## Appendix: TypeScript Interfaces

```typescript
// Enums
type DietType = "VEGAN" | "VEGETARIAN" | "CARNIVORE" | "NONE";
type PizzaType = "TEMPLATE" | "CUSTOM";
type ErrorCode = "INVALID_ARGUMENT" | "RESOURCE_NOT_FOUND" | "UNAUTHORIZED" | "DOWNSTREAM_ERROR" | "INTERNAL_ERROR";

// Request DTOs
interface UserRegisterRequest {
  name: string;
  email: string;
  password: string;
}

interface UserLoginRequest {
  email: string;
  password: string;
}

interface UserVerifyEmailRequest {
  token: string;
}

interface UserProfileUpdateRequest {
  name?: string;
  phone?: string;
}

interface DietPreferenceUpdateRequest {
  diet: DietType;
}

interface PreferredIngredientRequest {
  ingredientId: string; // UUID
}

interface PizzaSuitabilityRequest {
  pizzaId: string; // UUID
  additionalIngredientIds?: string[] | null;
  removedIngredientIds?: string[] | null;
}

interface PizzaScoreCreateRequest {
  pizzaId: string; // UUID
  pizzaType: PizzaType;
  score: number; // 1-5
  comment?: string | null;
}

interface ServiceFeedbackRequest {
  message: string;
  rating?: number | null; // 1-5
  category?: string | null;
}

// Response DTOs
interface UserRegisterResponse {
  userId: string; // UUID
  emailVerified: boolean;
  verificationToken: string;
}

interface UserLoginResponse {
  accessToken: string;
}

interface UserProfileResponse {
  id: string; // UUID
  name: string;
  email: string;
  emailVerified: boolean;
  preferredDiet: DietType;
  preferredIngredientIds: string[]; // UUID[]
  createdAt: string; // ISO-8601
  updatedAt: string; // ISO-8601
}

interface DietPreferenceResponse {
  diet: DietType;
}

interface IngredientIdResponse {
  ingredientId: string; // UUID
}

interface MenuResponse {
  sections: MenuSectionResponse[];
  pizzaCustomisations: PizzaCustomisationResponse[];
}

interface MenuSectionResponse {
  id: string; // UUID
  code: string;
  translationKey: string;
  sortOrder: number;
  items: MenuItemResponse[];
}

interface MenuItemResponse {
  id: string; // UUID
  sectionId: string; // UUID
  dishNumber: number;
  nameKey: string;
  descriptionKey: string;
  priceInSek: string; // decimal
  familySizePriceInSek: string; // decimal
  ingredients: MenuIngredientResponse[];
  sortOrder: number;
}

interface MenuIngredientResponse {
  id: string; // UUID
  ingredientKey: string;
  dietaryType: string;
  allergenTags: string[];
  spiceLevel: number;
}

interface PizzaCustomisationResponse {
  id: string; // UUID
  nameKey: string;
  priceInSek: string; // decimal
  familySizePriceInSek: string; // decimal
  sortOrder: number;
}

interface PizzaSummaryResponse {
  id: string; // UUID
  dishNumber: number;
  nameKey: string;
  priceInSek: string; // decimal
  familySizePriceInSek: string; // decimal
  sortOrder: number;
}

interface PizzaDetailResponse {
  id: string; // UUID
  dishNumber: number;
  nameKey: string;
  descriptionKey: string;
  priceInSek: string; // decimal
  familySizePriceInSek: string; // decimal
  ingredientKeys: string[];
  sortOrder: number;
}

interface PizzaSuitabilityResponse {
  suitable: boolean;
  violations: string[];
  suggestions: string[];
}

interface PizzaScoreResponse {
  id: string; // UUID
  userId: string; // UUID
  pizzaId: string; // UUID
  pizzaType: PizzaType;
  score: number;
  comment: string | null;
  createdAt: string; // ISO-8601
}

interface FeedbackResponse {
  id: string; // UUID
  userId: string; // UUID
  type: string;
  message: string;
  rating: number | null;
  createdAt: string; // ISO-8601
}

// Error Response (RFC 7807)
interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string | null;
  errorCode: ErrorCode;
  timestamp: string; // ISO-8601
  message?: string;
}
```

---

*Document generated: 2024-01-15*
*API Version: v1*
*Service: pizzeria-service*
