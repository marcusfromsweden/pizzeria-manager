# Coding Patterns & Pitfalls

Reusable patterns, common pitfalls, and their solutions for the pizzeria platform. These are
lessons learned from development — reference them to avoid repeating mistakes.

**Last Updated:** 2026-02-11

---

## Table of Contents

- [Frontend Patterns](#frontend-patterns)
  - [NaN Prevention in Numeric Inputs](#nan-prevention-in-numeric-inputs)
  - [React Fragment Keys in Lists](#react-fragment-keys-in-lists)
  - [Cache-Busting URLs](#cache-busting-urls)
  - [Async Data Loading Before Dialogs](#async-data-loading-before-dialogs)
  - [useEffect Dependency Pitfalls](#useeffect-dependency-pitfalls)
  - [React Query Cache Invalidation](#react-query-cache-invalidation)
  - [Axios 401 Interceptor Pattern](#axios-401-interceptor-pattern)
  - [Form Accessibility](#form-accessibility)
  - [Translation Key Fallback](#translation-key-fallback)
  - [localStorage Per-Tenant Pattern](#localstorage-per-tenant-pattern)
- [Backend Patterns](#backend-patterns)
  - [Reactive Error Handling Chain](#reactive-error-handling-chain)
  - [Repository Adapter Pattern](#repository-adapter-pattern)
  - [Time Abstraction for Testing](#time-abstraction-for-testing)
  - [Domain Validation vs Input Validation](#domain-validation-vs-input-validation)
  - [Multi-Tenant Data Isolation](#multi-tenant-data-isolation)
  - [Builder Pattern for Immutable Records](#builder-pattern-for-immutable-records)
- [Cross-Cutting Patterns](#cross-cutting-patterns)
  - [API Error Response Structure](#api-error-response-structure)
  - [Translation Key Convention](#translation-key-convention)
  - [Enum Consistency](#enum-consistency)

---

## Frontend Patterns

### NaN Prevention in Numeric Inputs

**Problem:** `parseInt()` and `parseFloat()` return `NaN` when the user clears a number input
field. If saved, `NaN` propagates through the system and corrupts data.

**Pattern:**
```typescript
const handleNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  let value = parseInt(e.target.value, 10);
  if (isNaN(value)) {
    value = defaultValue; // or minimum valid value
  }
  setValue(value);
};
```

**Where this applies:** Score inputs (1-5), rating inputs, quantity fields, any numeric form field.

**Key rule:** Always check `isNaN()` after `parseInt()` / `parseFloat()` and fall back to a safe
default.

---

### React Fragment Keys in Lists

**Problem:** When rendering multiple elements per list item (e.g., two `<TableRow>`s for a main row
+ expandable detail row), using `<>...</>` shorthand causes "missing key" warnings.

**Wrong:**
```typescript
{items.map(item => (
  <>
    <TableRow>...</TableRow>
    <TableRow>...</TableRow>
  </>
))}
```

**Correct:**
```typescript
{items.map(item => (
  <React.Fragment key={item.id}>
    <TableRow>...</TableRow>
    <TableRow>...</TableRow>
  </React.Fragment>
))}
```

**Rule:** Always use `<React.Fragment key={...}>` (not `<>`) when the Fragment is inside a `.map()`.

---

### Cache-Busting URLs

**Problem:** Browsers cache resources aggressively. After regenerating or updating a resource (e.g.,
an exported CSV, a generated file), the browser may serve the stale cached version.

**Pattern:**
```typescript
const url = `/api/v1/admin/pizzerias/${code}/prices/export?t=${Date.now()}`;
```

**When to use:**
- Downloading generated files that may be regenerated
- Fetching resources that bypass React Query caching
- Any URL where the same path returns different content over time

**When NOT needed:** React Query manages its own cache. Only use this for direct URL fetches
(`window.open`, `<a href>`, `fetch` without React Query).

---

### Async Data Loading Before Dialogs

**Problem:** Opening a dialog/modal that depends on async data (e.g., dropdown options) before the
data is loaded results in empty selects, missing options, or race conditions.

**Wrong:**
```typescript
const handleOpen = (item: Item) => {
  setDialogOpen(true);
  fetchOptions(); // Race condition: dialog renders before data arrives
};
```

**Correct:**
```typescript
const handleOpen = async (item: Item) => {
  try {
    const options = await fetchOptions();
    setOptions(options);
    setDialogOpen(true); // Only open after data is ready
  } catch (err) {
    setError('Failed to load data');
  }
};
```

**Alternative with React Query:**
```typescript
const { data: options, isLoading } = useQuery({
  queryKey: ['options'],
  queryFn: fetchOptions,
});

// In dialog: show spinner while isLoading, render content when ready
```

---

### useEffect Dependency Pitfalls

**Problem:** `useEffect` with object/array dependencies triggers on every render because React
compares by reference, not by value. This can cause infinite loops.

**Wrong:**
```typescript
useEffect(() => {
  doSomething(values);
}, [values]); // If `values` is a new object each render → infinite loop
```

**Solutions:**

1. **Use primitive dependencies:**
   ```typescript
   useEffect(() => {
     doSomething(values);
   }, [values.id, values.name]); // Primitives are compared by value
   ```

2. **Serialize for comparison:**
   ```typescript
   const valuesKey = JSON.stringify(values);
   useEffect(() => {
     doSomething(values);
   }, [valuesKey]);
   ```

3. **Use useCallback/useMemo to stabilize references:**
   ```typescript
   const stableValues = useMemo(() => computeValues(input), [input]);
   useEffect(() => {
     doSomething(stableValues);
   }, [stableValues]);
   ```

---

### React Query Cache Invalidation

**Pattern for mutations that should refresh related queries:**

```typescript
const mutation = useMutation({
  mutationFn: updateProfile,
  onSuccess: () => {
    // Invalidate the profile query so it refetches
    queryClient.invalidateQueries({ queryKey: ['profile'] });
    // Optionally invalidate related queries
    queryClient.invalidateQueries({ queryKey: ['preferences'] });
  },
});
```

**Key rules:**
- Always invalidate after mutations that change server state
- Use structured query keys: `['entity']`, `['entity', id]`, `['entity', 'list']`
- `invalidateQueries` marks data as stale; it refetches when the query is next observed
- For optimistic updates, use `onMutate` to update cache immediately

---

### Axios 401 Interceptor Pattern

**Pattern used in this project:**

```typescript
// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear token
      clearAuthToken();
      // Dispatch event so AuthProvider can react
      window.dispatchEvent(new CustomEvent('unauthorized'));
    }
    return Promise.reject(error);
  }
);
```

**Why a custom event?** The Axios client is outside React's component tree. A custom DOM event
bridges the gap to the AuthProvider context, which listens for `unauthorized` events and clears
auth state.

---

### Form Accessibility

**Problem:** MUI and native form elements warn about missing `id` and `name` attributes.

**Pattern:** Always include `id` and `name` on form controls:

```typescript
<input
  id={`field-${fieldName}`}
  name={fieldName}
  type="number"
  value={value}
  onChange={handleChange}
/>
```

For dynamic lists:
```typescript
<Select
  id={`select-${item.id}`}
  name={`diet-${item.id}`}
  value={value}
>
```

---

### Translation Key Fallback

**Pattern used by `useTranslateKey`:**

```typescript
const translateKey = (key: string, fallback?: string): string => {
  const translated = t(`menu:${key}`);
  if (translated !== key) return translated;   // Translation found
  if (fallback) return fallback;               // Use provided fallback

  // Format the key itself as readable text
  const lastPart = key.split('.').pop() || key;
  return lastPart
    .replace(/_/g, ' ')
    .replace(/\b\w/g, c => c.toUpperCase());
};
```

**Example:** `translation.key.disc.pizza.margarita` → looks up in i18n → if missing, returns
`"Margarita"`.

---

### localStorage Per-Tenant Pattern

**Pattern for multi-tenant data in localStorage:**

```typescript
// Auth tokens
const TOKEN_KEY = `pizzeria-${pizzeriaCode}-auth-token`;
localStorage.setItem(TOKEN_KEY, token);

// Cart items
const CART_KEY = `pizzeria-cart-${pizzeriaCode}`;
localStorage.setItem(CART_KEY, JSON.stringify(items));
```

**Why per-tenant?** Users can have accounts on multiple pizzerias. Scoping storage by pizzeria code
prevents data from one tenant leaking into another.

---

## Backend Patterns

### Reactive Error Handling Chain

**Pattern for handling "not found" in reactive chains:**

```java
return userRepository.findByIdAndPizzeriaId(userId, pizzeriaId)
    .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
    .flatMap(user -> {
        // Business logic with the user
        return doSomething(user);
    })
    .onErrorMap(ex -> {
        if (ex instanceof DomainValidationException) return ex;
        return new RuntimeException("Unexpected error", ex);
    });
```

**Key rules:**
- Use `switchIfEmpty()` to convert empty Mono to error
- Use `flatMap()` for dependent async operations (not `map()`)
- Use `onErrorMap()` to transform exceptions
- Never use `.block()` in production code (blocks the event loop)

---

### Repository Adapter Pattern

**Pattern used in this project:**

```
service/repository/UserRepository.java          (abstract interface)
service/repository/r2dbc/UserRepositoryAdapter.java  (R2DBC implementation)
test/.../inmemory/InMemoryUserRepository.java    (test stub)
```

**Benefits:**
- Domain layer doesn't depend on persistence technology
- Unit tests use fast in-memory stubs (ConcurrentHashMap)
- Integration tests use real R2DBC adapter with Testcontainers
- Easy to swap persistence (e.g., add Redis caching layer)

**Rule:** Service classes depend on the abstract interface, never on the adapter directly.

---

### Time Abstraction for Testing

**Pattern:**

```java
// Interface
public interface TimeProvider {
    Instant now();
}

// Production
@Component
public class SystemTimeProvider implements TimeProvider {
    public Instant now() { return Instant.now(); }
}

// Test
public class TestTimeProvider implements TimeProvider {
    private Instant fixedTime;
    public Instant now() { return fixedTime; }
    public void setTime(Instant time) { this.fixedTime = time; }
}
```

**Usage in services:**
```java
private final TimeProvider timeProvider;

public User register(...) {
    return User.builder()
        .createdAt(timeProvider.now())
        .build();
}
```

**Benefits:** Tests can assert exact timestamps without flaky time-dependent assertions.

---

### Domain Validation vs Input Validation

**Two distinct layers:**

1. **Input validation** (Jakarta / `@Valid` on DTOs):
   - Runs automatically on `@RequestBody`
   - Checks format: `@Email`, `@Size`, `@NotBlank`, `@Min`, `@Max`
   - Returns 400 Bad Request
   - Example: "password must be at least 8 characters"

2. **Domain validation** (in service layer):
   - Business rules checked in code
   - Throws `DomainValidationException` → 422 Unprocessable Entity
   - Example: "email already registered for this pizzeria"
   - Example: "cannot cancel a delivered order"

**Rule:** Input validation catches malformed requests. Domain validation enforces business rules.
Don't mix them — a valid JSON body can still violate business rules.

---

### Multi-Tenant Data Isolation

**Pattern for public endpoints:**
```java
@GetMapping("/pizzerias/{pizzeriaCode}/pizzas")
public Flux<PizzaResponse> listPizzas(@PathVariable String pizzeriaCode) {
    UUID pizzeriaId = pizzeriaService.resolvePizzeriaId(pizzeriaCode);
    return pizzaService.list(pizzeriaId);
}
```

**Pattern for authenticated endpoints:**
```java
@GetMapping("/users/me")
public Mono<UserProfileResponse> getProfile(AuthenticatedUser user) {
    // pizzeriaId comes from the JWT, not from the URL
    return userService.me(user.getUserId(), user.getPizzeriaId());
}
```

**Key rule:** Never trust the URL for tenant resolution on authenticated endpoints. The JWT is the
source of truth. This prevents users from accessing data in other tenants.

---

### Builder Pattern for Immutable Records

**Pattern:**
```java
@Builder(toBuilder = true)
public record User(
    UUID id,
    UUID pizzeriaId,
    String name,
    // ... more fields
) {}

// Create new
User user = User.builder()
    .id(UUID.randomUUID())
    .name("Alice")
    .build();

// Modify (creates a copy)
User updated = user.toBuilder()
    .name("Bob")
    .updatedAt(timeProvider.now())
    .build();
```

**Benefits:**
- Records are immutable (thread-safe, no accidental mutation)
- `toBuilder()` enables safe modification without mutating the original
- Lombok `@Builder` generates the builder at compile time

---

## Cross-Cutting Patterns

### API Error Response Structure

All API errors follow RFC 7807 ProblemDetail:

```json
{
  "type": "about:blank",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Pizza with id abc123 not found",
  "instance": "/api/v1/pizzerias/ramonamalmo/pizzas/abc123",
  "errorCode": "RESOURCE_NOT_FOUND",
  "timestamp": "2026-02-11T12:00:00Z"
}
```

**Frontend extraction:**
```typescript
export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.detail || error.message;
  }
  return 'An unexpected error occurred';
}
```

---

### Translation Key Convention

Backend returns translation keys; frontend resolves them.

| Pattern | Example | Resolves To |
|---------|---------|-------------|
| Pizza names | `translation.key.disc.pizza.margarita` | "Margherita" |
| Sections | `translation.key.section.pizzas` | "Pizzas" |
| Ingredients | `translation.key.ingredient.cheese` | "Cheese" |
| Customisations | `translation.key.pizza.customisation.extra_cheese` | "Extra Cheese" |
| Descriptions | `translation.key.description.homemade_lasagna` | "Homemade lasagna with..." |

**Locale files:** `src/i18n/locales/{en,sv}/menu.json`

**If adding a new menu item:** Add the translation key to both `en/menu.json` and `sv/menu.json`.

---

### Enum Consistency

Enums must match between backend and frontend:

| Enum | Backend (Java) | Frontend (TypeScript) | Values |
|------|---------------|----------------------|--------|
| Diet | `Diet` | `DietType` | VEGAN, VEGETARIAN, CARNIVORE, NONE |
| Order status | `OrderStatus` | `OrderStatus` | PENDING, CONFIRMED, PREPARING, READY, OUT_FOR_DELIVERY, DELIVERED, PICKED_UP, CANCELLED |
| Fulfillment | `FulfillmentType` | `FulfillmentType` | PICKUP, DELIVERY |
| Pizza size | `PizzaSize` | `PizzaSize` | REGULAR, FAMILY |
| Feedback kind | `FeedbackKind` | — | SERVICE, PRODUCT, DELIVERY |
| Feedback status | `FeedbackStatus` | — | OPEN, CLOSED, IN_PROGRESS |

**Rule:** When adding a new enum value on the backend, update the corresponding TypeScript type in
`src/types/api.ts`. The frontend must handle all possible values, including graceful fallback for
unknown values received from a newer backend.
