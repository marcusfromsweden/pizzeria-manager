# Third-Party Food Delivery Platform APIs for Restaurant Integration (2026)

## 1. Foodora (Delivery Hero)

Foodora operates in Sweden, Finland, Norway, Austria, Hungary, and Czechia. It is owned by Delivery Hero, and its restaurant integration APIs come from two sources: the legacy Delivery Hero Integration Middleware and the newer Foodora Partner API.

### API Access Model: Partner-only (not public)

Restaurants must be onboarded as partners. Credentials are obtained through the Foodora Vendor Portal or via a local Delivery Hero representative. There is no self-service developer signup.

### Authentication

- **Foodora Partner API**: OAuth 2.0 Client Credentials Flow. Generate an `access_token` by calling `https://foodora.partner.deliveryhero.io/v2/oauth/token` using `client_id` and `client_secret` generated in the Partner Portal under Shop Integrations.
- **Delivery Hero POS Middleware**: Credentials are provided after submitting a valid Public PGP Key to a local representative. The plugin URL must support HTTPS with a valid SSL certificate.

### Menu Synchronization

There are two approaches:

**Legacy Delivery Hero POS Middleware (XML-based, push model):**
- The middleware requests a menu from your plugin via `GET /menuimport/{chainVendorId}`.
- Your plugin generates the XML menu asynchronously, then pushes it to the middleware via `POST /v2/chains/{chainCode}/remoteVendors/{chainVendorId}/menuImport`.
- Menu format is defined by an XML Schema. Only XML is supported in this path.
- A callback webhook (`Webhook Status of Catalog Import`) notifies the plugin of the import result.

**Foodora Partner API / Catalog API (newer, push model):**
- `PUT` to submit/update the catalog (product information: status, price, quantity).
- `POST` to export the full assortment.
- `GET` to retrieve listed products.
- This is a REST/JSON approach and is the preferred path for newer integrations.

### Order Flow

**Delivery Hero POS Middleware:**
- Two modes: **Indirect** (orders accepted on Foodora tablet first, then forwarded to POS) and **Direct** (orders pushed directly from Foodora to your plugin without a tablet).
- In Direct mode, orders are pushed to your plugin endpoint. You must accept or reject via `POST Update Order Status` on the middleware API.

**Foodora Partner API (webhook-based):**
- Your system registers a webhook endpoint.
- New orders arrive as `POST` webhook notifications.
- Order statuses: `RECEIVED` -> `READY_FOR_PICKUP` -> `DISPATCHED` (or `CANCELED`).
- You update order status via the API, and receive cancellation notifications via webhooks.

### Order Status Updates

Push-based via webhooks (your system receives status changes) and you push status updates back via REST calls (e.g., marking an order as `READY_FOR_PICKUP`).

### Documentation

- [Foodora Partner API](https://developer.foodora.com/api-specifications)
- [Foodora Catalog API Integration Guide](https://developer.foodora.com/en/documentation/catalog-api-how-to-integrate)
- [Delivery Hero POS Documentation](https://developers.deliveryhero.com/documentation/pos.html)
- [Delivery Hero POS Plugin API (Swagger)](https://integration-middleware.stg.restaurant-partners.com/apidocs/pos-plugin-api)
- [Delivery Hero POS Middleware API (Swagger)](https://integration-middleware.stg.restaurant-partners.com/apidocs/pos-middleware-api)
- [Delivery Hero Menu API](https://developers.deliveryhero.com/documentation/menu.html)

---

## 2. Uber Eats

Uber Eats has the most mature and well-documented public developer API among the platforms researched.

### API Access Model: Partner-only with formal approval process

You need an Uber Eats partner manager, must sign an NDA and API licensing agreement, and begin with a Sandbox environment. Production access requires a 99% injection success rate.

### Authentication

OAuth 2.0 with scoped permissions. Endpoints require a Bearer token generated via Client Credentials with scopes like `eats.store` (menu), `eats.order` (orders).

### Menu Synchronization (Push model)

- `PUT /eats/stores/{store_id}/menus` -- Full menu upload/replacement.
- `GET /eats/stores/{store_id}/menus` -- Retrieve current menu.
- `POST /menus/items` -- Update individual items (out-of-stock/back-in-stock, pricing). Only works if the original menu was uploaded via API.

Menu JSON structure uses four entity types:
1. **Item** -- Individual sellable products (pizza, drink, topping).
2. **Modifier Group** -- Groups items as customizations (e.g., "Pizza Toppings" with "Mushroom", "Peppers").
3. **Category** -- Groups top-level items into sections (e.g., "Appetizers", "Main Courses").
4. **Menu** -- Groups categories with service availability hours.

Prices are in cents. Translations are supported via `title.translations`. Menu updates reflect immediately (except images, which can take hours).

### Order Flow (Webhook-based with pull for details)

1. Customer places an order on Uber Eats.
2. Uber sends a webhook notification to your configured Primary Webhook URL with an `event_type` and `resource_href`.
3. You acknowledge the webhook (return HTTP 200).
4. You pull full order details using the `resource_href` URL.
5. You must call `POST /eats/orders/{order_id}/accept_pos_order` or `POST /eats/orders/{order_id}/deny_pos_order` within **11.5 minutes**. A robocall is triggered after 90 seconds without response.

Webhook security supports both Basic Auth and OAuth.

Retry logic: On 5xx errors or timeouts, retries start at 10 seconds, then exponential backoff (30s, 60s, 120s...) up to 7 total attempts.

### Order Status Updates

You push status updates back to Uber Eats via REST API calls at each stage of the order lifecycle.

### Additional APIs

- **Store Management**: Online/offline status, hours, settings.
- **Promotions API**: Create, read, delete promotions autonomously.
- **Dispatch Multi-Courier**: For large orders requiring 2-5 couriers.
- **Reporting API**: Transaction reports, performance metrics.

### Documentation

- [Uber Eats Introduction](https://developer.uber.com/docs/eats/introduction)
- [Getting Started Guide](https://developer.uber.com/docs/eats/guides/getting-started)
- [Menu Integration Guide](https://developer.uber.com/docs/eats/guides/menu-integration)
- [Webhooks Guide](https://developer.uber.com/docs/eats/guides/webhooks)
- [API Change Log](https://developer.uber.com/docs/eats/api-change-log)
- [Postman Collection](https://www.postman.com/uber/uber-eats-marketplace-api/overview)

---

## 3. Wolt

Wolt (acquired by DoorDash in 2022) operates in 25 countries including Sweden. It has a well-structured developer portal with separate Menu, Order, and Venue APIs.

### API Access Model: Partner-only

Access is obtained through a Wolt account manager or technical account manager. There is no self-service signup. Credentials are provided for staging during development and for production after passing the testing phase.

### Authentication

OAuth 2.0 with JWT Bearer tokens. All requests must include `Authorization: Bearer <token>`. For Wolt Drive, a single "Merchant Key" token per merchant is used. The API key and client secret must be stored securely and should be easily rotatable.

### Menu Synchronization (Push model)

- `POST /v1/restaurants/{venueId}/menu` -- Push a full menu. **This is destructive**: it erases the entire existing menu and creates a new one. Specials, custom commissions, and manual edits are lost.
- `PATCH /venues/{venueId}/items/inventory` -- Update item stock/availability.
- `PATCH /venues/{venueId}/items` -- Update item prices, discounted prices.
- `PATCH /venues/{venueId}/options/values` -- Update option/modifier prices and availability.

Menu JSON structure:
- `id` -- Unique menu identifier.
- `currency` -- ISO 4217 currency code (e.g., `SEK`).
- `language` -- ISO 639-1 language code (e.g., `sv`).
- `categories` -- Category groupings.
- `items` -- Products with option bindings.
- `options` -- Modifiers/customizations.

Menus are processed asynchronously and reflect in under a minute.

### Order Flow (Webhook + pull model)

1. Customer places order on Wolt.
2. Wolt sends an `order_created` webhook to your server. The notification includes a `resource_url` but **not** full order details.
3. You verify the webhook using HMAC-SHA256 (with your client secret, checking `wolt-signature` header).
4. You fetch full order details from the `resource_url`.
5. You accept the order via the accept endpoint (can be automatic or manual).
6. Subsequent status changes come as webhook notifications.

### Order Types

- **Instant Order**: Delivered by Wolt courier (ASAP).
- **Takeaway Order**: Customer picks up at restaurant.
- **Self-Delivery Order**: Restaurant handles its own delivery.

### Venue API

- Monitor and manage online status and opening hours.
- Set venues online/offline programmatically.

### Documentation

- [Wolt Developer Portal - Getting Started](https://developer.wolt.com/docs/getting-started/restaurant)
- [Menu API Reference](https://developer.wolt.com/docs/api/menu)
- [Order API Reference](https://developer.wolt.com/docs/api/order)
- [Restaurant Menu Guide](https://developer.wolt.com/docs/menuguide)
- [Restaurant Order Guide](https://developer.wolt.com/docs/orderrestaurantguide)
- [Restaurant Self-Delivery Integration](https://developer.wolt.com/docs/marketplace-integrations/restaurant-self-delivery)
- [Restaurant iPad-Free Integration](https://developer.wolt.com/docs/marketplace-integrations/restaurant-ipad-free)

---

## 4. Middleware / Aggregator Platforms

These platforms sit between your restaurant system and multiple delivery platforms, providing a single API to manage all channels.

### 4.1 Deliverect

**The most relevant option for Sweden.** Deliverect has explicit integrations with Foodora, Wolt, and Uber Eats, and has Nordic POS partnerships (Trivec, Ancon).

- **Scale**: 70,000+ restaurant locations, 30 million API calls/day, 1+ billion orders processed.
- **Supported Platforms in Sweden**: Foodora, Wolt, Uber Eats, Foodora GO, Deliveroo, DoorDash, and many more.
- **Architecture**: Webhook-based. Your system registers webhook URLs during setup.

**Integration Flow:**
1. Register POS webhook URL -- Deliverect sends a registration webhook with `accountId`, `locationId`, etc.
2. Your system responds with webhook URLs: `ordersWebhookURL`, `syncProductsURL`, `operationsWebhookURL`, `storeStatusWebhookURL`.
3. Product sync -- Deliverect retrieves products from your system.
4. Menu publish -- You push menus to Deliverect, which syncs them to all connected delivery channels.
5. Order notification -- When a customer orders on any platform, Deliverect sends a webhook containing products, prices, payment info, and customer info.
6. Order status updates -- You send async status updates back to Deliverect, which relays them to the originating platform.

**Authentication**: API credentials with scoped access. HMAC header on webhooks for verification.

**Webhook Response**: Must return HTTP 200/201 within 30 seconds.

**Documentation:**
- [Deliverect Developer Hub](https://developers.deliverect.com/)
- [Building a POS Integration](https://developers.deliverect.com/docs/building-a-pos-integration)
- [Partner Webhooks](https://developers.deliverect.com/reference/pos_webhooks)
- [Order Flow](https://developers.deliverect.com/docs/order-flow)
- [Order Notification Webhook](https://developers.deliverect.com/reference/post-orders-webhook)
- [Menu Sync](https://developers.deliverect.com/docs/how-do-i-receive-a-customer-menu)
- [Postman Collection](https://www.postman.com/deliverect/api-team/documentation/edifiys/deliverect-public-api)

### 4.2 Otter (formerly Hubster)

- Aggregates orders from multiple delivery platforms.
- Has historically used web-scraping (not official APIs) for some integrations, now transitioning to official APIs.
- Owns FutureFoods (virtual restaurant brands), which is a potential conflict of interest.
- Part of DoorDash's preferred integration partner list.
- [Otter Integrations](https://www.tryotter.com/integrations)

### 4.3 Other Middleware Providers

| Provider | Notes |
|---|---|
| **Chowly** | US-focused, DoorDash preferred partner |
| **Checkmate** | DoorDash preferred partner |
| **Cuboh** | Alternative to Deliverect/Otter |
| **UrbanPiper** | 35,000+ businesses, strong in India/Middle East/UK. **Not confirmed in Sweden/Nordics.** |
| **MERGEPORT** | Explicitly supports Foodora integration in Direct and Indirect modes |
| **GetOrder** | Integrates Foodora, Wolt with POS systems |

---

## 5. Comparison Matrix

| Feature | Foodora (DH) | Uber Eats | Wolt | Deliverect (Middleware) |
|---|---|---|---|---|
| **API Type** | REST (JSON + legacy XML) | REST (JSON) | REST (JSON) | REST (JSON) + Webhooks |
| **Public API** | No (partner-only) | No (partner + NDA) | No (partner-only) | Yes (partner developer hub) |
| **Auth Model** | OAuth 2.0 Client Credentials | OAuth 2.0 with scopes | OAuth 2.0 + JWT Bearer | API credentials + HMAC |
| **Menu Sync** | Push (PUT catalog / XML) | Push (PUT full menu) | Push (POST full menu) | Bidirectional (webhook + pull) |
| **Menu Format** | JSON (new) / XML (legacy) | JSON (items, modifiers, categories, menus) | JSON (categories, items, options) | JSON (normalized) |
| **Partial Menu Update** | Yes (price/status/qty) | Yes (POST /menus/items) | Yes (PATCH items, options, inventory) | Yes (via product sync) |
| **Order Delivery** | Webhook (push) | Webhook (push) | Webhook (push) | Webhook (push) |
| **Order Details** | In webhook payload | Pull via resource_href | Pull via resource_url | In webhook payload |
| **Order Accept/Reject** | REST call back | POST accept/deny (11.5 min timeout) | REST call (auto or manual) | Async status update |
| **Status Updates** | REST push to platform | REST push to platform | REST push + webhook receive | REST push to Deliverect |
| **Webhook Security** | SSL + PGP (legacy) / OAuth (new) | Basic Auth or OAuth | HMAC-SHA256 | HMAC signature |
| **Sandbox/Testing** | Via partner portal | Yes (dedicated sandbox) | Staging environment | Yes (test environment) |
| **Sweden Support** | Yes (primary market) | Yes | Yes | Yes (via Trivec, Ancon) |

---

## 6. Open-Source Implementations

There are **no production-quality open-source libraries** specifically for integrating with Foodora, Uber Eats, or Wolt restaurant APIs. The GitHub projects found are food delivery platform clones (building your own Uber Eats), not integration adapters.

However, relevant architectural patterns exist:

- **AWS Reference Architecture**: Event-driven integration of QSR systems with delivery aggregators using EventBridge, Lambda, and Step Functions. See [AWS Blog: Integrating aggregators and QSRs with serverless architectures](https://aws.amazon.com/blogs/compute/integrating-aggregators-and-quick-service-restaurants-with-aws-serverless-architectures/).
- **Adapter Pattern**: The most common approach is an adapter/facade layer that normalizes different delivery platform APIs behind a unified internal interface.

---

## 7. Recommended Architecture for Your Pizzeria Service

Given that your Spring Boot WebFlux backend already has menu items, orders, and order status tracking, here is the recommended integration approach:

### Option A: Direct Integration (More control, more work)

Build adapter modules for each platform:

```
pizzeria-service/
  └── service/
      └── delivery/
          ├── DeliveryPlatformAdapter.java          (interface)
          ├── DeliveryOrderNormalizer.java           (maps platform orders to your domain)
          ├── uber/
          │   ├── UberEatsAdapter.java               (implements adapter)
          │   ├── UberEatsWebhookController.java     (receives webhooks)
          │   └── UberEatsMenuMapper.java            (maps your menu to Uber format)
          ├── wolt/
          │   ├── WoltAdapter.java
          │   ├── WoltWebhookController.java
          │   └── WoltMenuMapper.java
          └── foodora/
              ├── FoodoraAdapter.java
              ├── FoodoraWebhookController.java
              └── FoodoraMenuMapper.java
```

Each adapter would:
1. Expose a webhook endpoint for incoming orders (reactive `Mono<ServerResponse>`).
2. Map incoming orders to your internal domain model.
3. Push menu updates to the platform when your menu changes.
4. Push order status updates back to the platform.
5. Use WebClient (already in your stack) for outbound REST calls.

### Option B: Middleware Integration via Deliverect (Less work, recurring cost)

Integrate only with Deliverect's API:
1. Build one webhook endpoint that receives orders from Deliverect (covering Foodora, Wolt, and Uber Eats simultaneously).
2. Push your menu to Deliverect once; it syncs to all connected platforms.
3. Send order status updates to Deliverect; it relays to the originating platform.

This reduces the integration surface from three separate APIs to one, at the cost of a per-order fee to Deliverect.

### Key Technical Considerations for Spring Boot WebFlux

- **Webhook Endpoints**: Use `@RestController` with `Mono<ResponseEntity>` return types. Webhook responses must be fast (under 30 seconds for Deliverect, under 11.5 minutes for Uber Eats order accept/deny).
- **HMAC Verification**: Implement a `WebFilter` or `HandlerFilterFunction` to verify HMAC signatures on incoming webhooks (Wolt uses HMAC-SHA256, Deliverect uses HMAC, Uber supports Basic Auth/OAuth).
- **Menu Push**: Use `WebClient` to push menu updates reactively when menu items are modified in your system.
- **Retry Logic**: Use `Mono.retryWhen()` with exponential backoff for outbound API calls.
- **OAuth Token Management**: Implement a token cache with automatic refresh for each platform's OAuth 2.0 tokens.

---

## Sources

- [Uber Eats Marketplace APIs - Introduction](https://developer.uber.com/docs/eats/introduction)
- [Uber Eats Getting Started Guide](https://developer.uber.com/docs/eats/guides/getting-started)
- [Uber Eats Webhooks Guide](https://developer.uber.com/docs/eats/guides/webhooks)
- [Uber Eats Menu Integration Guide](https://developer.uber.com/docs/eats/guides/menu-integration)
- [Uber Eats API Change Log](https://developer.uber.com/docs/eats/api-change-log)
- [Uber Eats Postman Collection](https://www.postman.com/uber/uber-eats-marketplace-api/overview)
- [Delivery Hero POS Documentation](https://developers.deliveryhero.com/documentation/pos.html)
- [Delivery Hero POS Plugin API (Swagger)](https://integration-middleware.stg.restaurant-partners.com/apidocs/pos-plugin-api)
- [Delivery Hero POS Middleware API (Swagger)](https://integration-middleware.stg.restaurant-partners.com/apidocs/pos-middleware-api)
- [Delivery Hero Menu API](https://developers.deliveryhero.com/documentation/menu.html)
- [Foodora Partner API](https://developer.foodora.com/api-specifications)
- [Foodora Catalog API Integration Guide](https://developer.foodora.com/en/documentation/catalog-api-how-to-integrate)
- [Foodora Partners Page](https://www.foodora.com/partners/)
- [Wolt Developer Portal - Getting Started](https://developer.wolt.com/docs/getting-started/restaurant)
- [Wolt Menu API Reference](https://developer.wolt.com/docs/api/menu)
- [Wolt Order API Reference](https://developer.wolt.com/docs/api/order)
- [Wolt Restaurant Menu Guide](https://developer.wolt.com/docs/menuguide)
- [Wolt Restaurant Order Guide](https://developer.wolt.com/docs/orderrestaurantguide)
- [Wolt Restaurant Self-Delivery Integration](https://developer.wolt.com/docs/marketplace-integrations/restaurant-self-delivery)
- [Wolt Restaurant iPad-Free Integration](https://developer.wolt.com/docs/marketplace-integrations/restaurant-ipad-free)
- [Deliverect Developer Hub](https://developers.deliverect.com/)
- [Deliverect - Building a POS Integration](https://developers.deliverect.com/docs/building-a-pos-integration)
- [Deliverect - Partner Webhooks](https://developers.deliverect.com/reference/pos_webhooks)
- [Deliverect - Order Flow](https://developers.deliverect.com/docs/order-flow)
- [Deliverect - Order Notification Webhook](https://developers.deliverect.com/reference/post-orders-webhook)
- [Deliverect - Webhook Verification](https://developers.deliverect.com/docs/how-to-i-verify-orders-received-to-webhook)
- [Deliverect - Postman Collection](https://www.postman.com/deliverect/api-team/documentation/edifiys/deliverect-public-api)
- [Deliverect - Wolt Integration](https://www.deliverect.com/en/integrations/wolt)
- [Deliverect - Foodora Integration](https://www.deliverect.com/en/integrations/foodora)
- [Trivec + Deliverect Nordic Partnership](https://trivecgroup.com/news/trivec-partners-with-deliverect-to-integrate-all-food-delivery-apps-in-the-nordic-region/)
- [Otter Integrations](https://www.tryotter.com/integrations)
- [UrbanPiper](https://www.urbanpiper.com/)
- [Cuboh vs Deliverect vs Otter vs Chowly](https://www.cuboh.com/cuboh-vs-deliverect-vs-otter-vs-chowly)
- [AWS: Integrating Aggregators and QSRs with Serverless](https://aws.amazon.com/blogs/compute/integrating-aggregators-and-quick-service-restaurants-with-aws-serverless-architectures/)
- [MERGEPORT - Foodora Integration](https://www.mergeport.com/foodora-sales/)
- [GetOrder - Wolt Integration](https://getorder.biz/wolt/)
- [GetOrder - Foodora Integration](https://getorder.biz/foodora/)