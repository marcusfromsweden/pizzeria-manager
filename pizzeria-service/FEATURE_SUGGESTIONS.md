# Feature Suggestions for Pizzeria Platform

Prioritized list of features that would add value for users of the pizzeria platform. Each entry
notes what already exists in the codebase and what needs to be built.

---

## What Exists Today

The platform is a full-stack pizza ordering application (Spring Boot WebFlux backend + React/TypeScript
frontend) with:

- **Menu browsing** — Sections, items, ingredients, dietary badges, allergens, spice levels,
  customisations, regular + family pricing
- **User accounts** — Registration with email verification, login/logout, profile editing with avatar
  upload, account deletion
- **Ordering** — Cart, checkout with pickup/delivery selection, saved delivery addresses, order
  history, order detail, order cancellation
- **Dietary preferences** — Set diet type (vegan/vegetarian/carnivore/none), preferred ingredients,
  pizza suitability checker
- **Pizza ratings** — Rate pizzas 1–5 stars with comments, view rating history
- **Service feedback** — Submit feedback with rating and category, view admin replies with unread
  badges
- **Admin tools** — CSV price export/import with dry-run preview, feedback management with reply
  capability
- **Multi-tenancy** — Pizzeria-scoped data, configurable per tenant
- **i18n** — English and Swedish

---

## High Impact

These features address fundamental gaps in the ordering experience.

### 1. Payment Integration

**What it is**: Accept real payments for orders (Swish, Klarna, card).

**What exists**: Orders can be placed with full pricing (subtotal, delivery fee, total). Order items
track base price and customisation price. The checkout page calculates totals. No payment processing
exists.

**What's needed**:
- Backend: Payment provider integration (Stripe, Klarna, or Swish API), payment status on orders,
  webhook handlers for payment confirmations, refund support for cancelled orders
- Frontend: Payment form/redirect in checkout flow, payment status display on order detail
- Database: Payment records table (amount, provider, status, transaction ID)

**Dependencies**: None — can be built on top of existing order flow.

---

### 2. Real-Time Order Status Updates

**What it is**: Live updates when an order moves from Pending → Confirmed → Preparing → Ready →
Delivered/Picked Up.

**What exists**: `OrderStatus` enum with PENDING, CONFIRMED, READY, COMPLETED, CANCELLED states.
Orders have `estimatedReadyTime` field (never populated). Frontend has order detail page showing
status. No mechanism for the backend to push status changes.

**What's needed**:
- Backend: WebSocket or SSE endpoint for order status stream, admin/kitchen endpoint to update order
  status, populate `estimatedReadyTime`
- Frontend: Subscribe to status stream on order detail page, show progress indicator, optional
  browser notifications
- Consider: SSE is simpler with WebFlux (`Flux<ServerSentEvent>`), WebSocket offers bidirectional
  communication

**Dependencies**: More useful once there's a kitchen/admin order management interface.

---

### 3. Email Service & Notifications

**What it is**: Send real emails for verification, order confirmation, and password reset.

**What exists**: Email verification uses a token displayed on screen (development-only). No email
sending capability. Verification token stored in-memory.

**What's needed**:
- Backend: Email service (SendGrid, AWS SES, or SMTP), email templates for verification/order
  confirmation/password reset, async sending via reactive streams
- Frontend: Remove token display from verification page, show "check your email" message instead
- Infrastructure: Email provider account, DNS records (SPF, DKIM)

**Dependencies**: Required before password reset can work. Enables order confirmation emails.

---

### 4. Dish Images

**What it is**: Photos of menu items displayed on menu, pizza list, and detail pages.

**What exists**: No image support in the menu data model. Users already have profile photo support
(base64 in database). Menu items have names and descriptions but no images.

**What's needed**:
- Backend: Image storage (S3/MinIO or database BLOB), image upload endpoint (admin), image URL field
  on `menu_items` table, image serving endpoint or CDN
- Frontend: Display images on menu cards, pizza list, pizza detail page, cart items
- Database: Add `image_url` or `image_key` column to `menu_items`
- Data: Photograph or source images for all menu items

**Dependencies**: None, but requires image assets.

---

### 5. Password Reset

**What it is**: Allow users to reset forgotten passwords via email.

**What exists**: Login with email/password works. No reset flow exists — neither API endpoint nor UI.

**What's needed**:
- Backend: `POST /users/forgot-password` (generates reset token, sends email), `POST
  /users/reset-password` (validates token, updates password), token expiration
- Frontend: "Forgot password?" link on login page → email input form → token entry + new password
  form
- Database: Reset token storage (with expiration)

**Dependencies**: Requires email service (#3).

---

## Medium Impact

Features that improve discovery, engagement, and the overall user experience.

### 6. Menu Search

**What it is**: Search menu items by name, ingredient, or description.

**What exists**: Full menu with items, ingredients, and descriptions loaded from database. No search
endpoint or UI.

**What's needed**:
- Backend: `GET /api/v1/pizzerias/{code}/menu/search?q=...` endpoint with text matching on item
  names, descriptions, and ingredient keys
- Frontend: Search bar on menu page, search results display with highlighting

**Dependencies**: None.

---

### 7. Allergen & Dietary Filters

**What it is**: Filter menu items by dietary type or exclude allergens.

**What exists**: Every ingredient has `dietary_type` (VEGAN, VEGETARIAN, CARNIVORE) and
`allergen_tags` (dairy, gluten, nuts, shellfish, etc.) in `menu_ingredient_facts`. The
`POST /pizzas/suitability` endpoint checks individual pizzas. No bulk filter exists.

**What's needed**:
- Backend: Query parameter filters on menu endpoint (`?diet=VEGAN&excludeAllergens=dairy,gluten`)
- Frontend: Filter panel on menu page with checkboxes/toggles for diet types and allergens, filtered
  results display

**Dependencies**: None — data already exists in the database.

---

### 8. Public Pizza Reviews

**What it is**: Show pizza ratings and comments on pizza detail pages for all users.

**What exists**: `pizza_scores` table stores ratings (1–5) with comments per user per pizza.
`PizzaScoreController` has `POST` and `GET /me` endpoints. No public-facing aggregation or display.

**What's needed**:
- Backend: `GET /api/v1/pizzerias/{code}/pizzas/{id}/reviews` returning aggregated score (average,
  count) and recent reviews, include average rating in pizza list response
- Frontend: Star rating display on pizza cards and detail pages, reviews section on pizza detail
  page, pagination for reviews

**Dependencies**: None.

---

### 9. Pizzeria Info Page

**What it is**: Dedicated page showing opening hours, address, phone numbers, and a map.

**What exists**: Pizzeria config JSONB stores opening hours (with day-of-week time ranges), address
(street, postal code, city), and phone numbers. `OpeningHoursDisplay` component exists in frontend.
`fetchPizzeriaInfo` API call exists. Header shows open/closed status. No dedicated info page.

**What's needed**:
- Frontend: Info page at `/:pizzeriaCode/info` showing hours table, address, phone numbers,
  embedded map (Google Maps or OpenStreetMap iframe)
- Backend: Data already available via existing endpoint

**Dependencies**: None — mostly a frontend task.

---

### 10. Favorites & Quick Reorder

**What it is**: Save favorite pizzas or past orders for one-tap reordering.

**What exists**: `user_preferred_ingredients` tracks ingredient preferences. Order history with full
item details exists. No favorites or reorder mechanism.

**What's needed**:
- Backend: `favorite_pizzas` table (userId, menuItemId), CRUD endpoints, "reorder" endpoint that
  clones a past order into a new cart
- Frontend: Heart/star button on pizza cards and detail page, favorites list page, "Reorder" button
  on order history items
- Database: New `favorite_pizzas` table

**Dependencies**: None.

---

### 11. Estimated Delivery/Ready Time

**What it is**: Show customers when their order will be ready or delivered.

**What exists**: `Order` domain has `estimatedReadyTime` field (LocalDateTime). Never populated.
Order detail page exists but doesn't display it.

**What's needed**:
- Backend: Logic to calculate estimated time based on order size/queue, populate
  `estimatedReadyTime` on order creation or confirmation, update as order progresses
- Frontend: Display estimated time on order detail and active orders pages, countdown or time
  display

**Dependencies**: More useful with real-time order status (#2).

---

## Lower Impact

Polish and quality-of-life improvements.

### 12. Skeleton Loaders

**What it is**: Show content-shaped placeholders while data loads instead of generic spinners.

**What exists**: `Spinner` component used across all pages during loading states. React Query manages
loading states.

**What's needed**:
- Frontend: Skeleton components for menu cards, pizza cards, order list items, profile sections.
  Replace `Spinner` usage in key pages with skeletons.

**Dependencies**: None.

---

### 13. Error Boundaries

**What it is**: Catch JavaScript errors in component tree and show a recovery UI instead of a blank
page.

**What exists**: No React error boundary component. Unhandled rendering errors crash the entire app.

**What's needed**:
- Frontend: Error boundary component wrapping route content, fallback UI with "Something went wrong"
  message and retry/home button

**Dependencies**: None.

---

### 14. WCAG Accessibility Audit

**What it is**: Ensure the app meets basic accessibility standards (keyboard navigation, screen
reader support, color contrast).

**What exists**: Basic ARIA attributes on some components. Tailwind provides reasonable color
contrast. Not audited.

**What's needed**:
- Audit with axe-core or Lighthouse
- Fix: keyboard navigation on interactive elements, focus management on modals/dialogs, ARIA labels
  on form fields, skip-to-content link, proper heading hierarchy

**Dependencies**: None.

---

### 15. FAQ Section

**What it is**: Frequently asked questions page for common customer queries.

**What exists**: Nothing.

**What's needed**:
- Backend (optional): FAQ table with questions/answers, admin CRUD, or hardcode in frontend
- Frontend: FAQ page with collapsible question/answer sections

**Dependencies**: None.

---

### 16. Dedicated Delivery Address Management

**What it is**: A standalone page to manage saved delivery addresses outside of checkout.

**What exists**: Full address CRUD API (`DeliveryAddressController`). Addresses managed inline during
checkout only.

**What's needed**:
- Frontend: Address management section on profile page or standalone page, reuse existing API calls
  from checkout

**Dependencies**: None — backend API already exists.

---

## Business & Marketing

Features for growing the customer base and increasing order value.

### 17. Campaigns & Discount Codes

**What it is**: Promo codes, percentage/fixed discounts, time-limited campaigns.

**What exists**: Nothing.

**What's needed**:
- Backend: `campaigns` and `discount_codes` tables, validation endpoint, discount application in
  order pricing logic, campaign management admin API
- Frontend: Promo code input on checkout, active campaigns display on home/menu pages, admin campaign
  management page

**Dependencies**: None, but more impactful with payment integration (#1).

---

### 18. Loyalty Program

**What it is**: Points earned per order, redeemable for discounts or free items.

**What exists**: Nothing.

**What's needed**:
- Backend: Points ledger table, earning rules (per SEK spent), redemption rules, points balance
  endpoint
- Frontend: Points display in header/profile, redemption option at checkout, points history

**Dependencies**: More meaningful with payment integration (#1).

---

### 19. Newsletter / Email Marketing

**What it is**: Collect email opt-ins and send promotional emails.

**What exists**: User emails stored. No opt-in tracking or email sending.

**What's needed**:
- Backend: Newsletter opt-in field on users, integration with email marketing service (Mailchimp,
  SendGrid marketing), unsubscribe endpoint
- Frontend: Opt-in checkbox during registration, manage subscription in profile

**Dependencies**: Requires email service (#3).

---

### 20. Analytics

**What it is**: Track page views, user behavior, and conversion metrics.

**What exists**: Nothing.

**What's needed**:
- Frontend: Google Analytics 4 or Plausible/Umami integration, event tracking on key actions (add
  to cart, checkout, order placed)
- Backend (optional): Internal analytics endpoints for admin dashboard

**Dependencies**: Should be paired with GDPR cookie consent (#21).

---

### 21. GDPR Cookie Consent

**What it is**: Cookie consent banner and preference management.

**What exists**: Nothing. localStorage used for cart and auth tokens.

**What's needed**:
- Frontend: Cookie consent banner, preference storage, conditional loading of analytics scripts

**Dependencies**: Should be implemented alongside analytics (#20).

---

## Technical Debt

Issues that should be addressed for production readiness.

### 22. Rate Limiting

**What it is**: Protect login and registration endpoints from brute-force attacks.

**What exists**: No rate limiting. Any number of login attempts allowed.

**What's needed**:
- Backend: Rate limiter on auth endpoints (Spring Cloud Gateway rate limiter, Bucket4j, or custom
  WebFilter), IP-based and account-based limits, 429 Too Many Requests responses

**Dependencies**: None.

---

### 23. Token Storage Migration

**What it is**: Move auth tokens from in-memory ConcurrentHashMap to persistent storage.

**What exists**: `ConcurrentHashMap` storing active tokens. Tokens lost on restart. Won't work with
multiple service instances. Verification tokens also in-memory with no TTL (memory leak risk).

**What's needed**:
- Backend: Redis or database-backed token store, token TTL/expiration cleanup, or migrate to
  stateless JWT (no server-side token store needed)

**Dependencies**: None, but important before horizontal scaling.

---

### 24. Backend-Configurable Delivery Fee

**What it is**: Move the hardcoded 49 SEK delivery fee from the frontend to backend configuration.

**What exists**: `49` hardcoded in `CheckoutPage.tsx`. Backend calculates delivery fee in
`OrderService` (also hardcoded as `BigDecimal("49.00")`).

**What's needed**:
- Backend: Add `deliveryFee` to pizzeria config JSONB, use config value in `OrderService`
- Frontend: Fetch delivery fee from pizzeria info endpoint, remove hardcoded value

**Dependencies**: None.

---

## Feature Dependencies

```
Email Service (#3)
├── Password Reset (#5)
├── Order Confirmation Emails
└── Newsletter (#19)

Payment Integration (#1)
├── Campaigns & Discounts (#17) — more impactful with real payments
└── Loyalty Program (#18) — points based on spend

Real-Time Order Status (#2)
└── Estimated Delivery Time (#11) — more useful together

Analytics (#20)
└── GDPR Cookie Consent (#21) — should ship together
```

All other features are independent and can be built in any order.
