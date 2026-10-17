# QuickBite REST API Reference & Security Matrix

This document serves as the comprehensive API specification and developer reference for the QuickBite platform, indexing all 35+ REST controllers, role requirements, parameter specifications, and security policies.

---

## 1. Authentication & Security Model

All API endpoints (except explicitly public routes) are protected by Spring Security 6+ and OAuth 2.0 Resource Server filters.

### 1.1 Header & Cookie Specifications

| Credential Type | Protocol / Format | Transport Mechanism | Scope / Role Mapping | Lifetime |
| :--- | :--- | :--- | :--- | :--- |
| **API Access Token** | RS256 Signed JWT | `Authorization: Bearer <token>` | `SCOPE_API` + `ROLE_<ROLE>` | 15 Minutes (`PT15M`) |
| **Session Challenge Token** | RS256 Signed JWT | `Authorization: Bearer <token>` | `SCOPE_AUTH` only | 5 Minutes (`PT5M`) |
| **Refresh Token** | 64-Byte Opaque String | **Web**: Cookie `qb_refresh_token` (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`)<br/>**Mobile**: JSON Body (`AuthResponse.refreshToken` / `RefreshRequest.refreshToken`) | Internal validation against PostgreSQL & Redis | 7 Days (`P7D`) |
| **Client Context** | Mandatory Auth Header | `X-Client-Type: web_browser \| mobile_app` | Resolved by `YauaaDeviceInfoResolver` | Per-request on `/login`, `/refresh-token`, `/claim-session` |

---

## 2. Role-Based Access Control (RBAC) Matrix

| URL Pattern | Required Authority / Role | Permitted HTTP Methods | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth/register/**` | **PermitAll** (Public) | `POST` | Customer, driver & restaurant owner registration |
| `/api/v1/auth/login` | **PermitAll** (Public) | `POST` | User authentication |
| `/api/v1/auth/refresh-token` | **PermitAll** (Cookie or Body) | `POST` | Refresh token rotation |
| `/api/v1/webhooks/**` | **PermitAll** (Signature check) | `POST` | Gateway webhooks (Razorpay) |
| `/api/v1/cuisines` | **PermitAll** (Public) | `GET` | Browse approved cuisines |
| `/api/v1/restaurants/**` | **PermitAll** (Public) | `GET` | Browse restaurants, menus, reviews |
| `/actuator/health`, `/info` | **PermitAll** (Public) | `GET` | Liveness & Readiness probes |
| `/api/v1/auth/sessions/**` | `SCOPE_AUTH` | `GET`, `DELETE` | Session challenge resolution |
| `/api/v1/auth/claim-session` | `SCOPE_AUTH` | `POST` | Claim session on eviction |
| `/api/v1/auth/logout`, `/logout-all` | `SCOPE_API` | `POST` | Single-session or global logout |
| `/api/v1/files/upload-url` | `SCOPE_API` | `POST` | Direct S3 presigned upload URL |
| `/api/v1/customer/**` | `SCOPE_API` + `ROLE_CUSTOMER` | `GET`, `POST`, `PUT`, `DELETE` | Customer operations |
| `/api/v1/restaurant-owner/**` | `SCOPE_API` + `ROLE_RESTAURANT_OWNER` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | Restaurant management |
| `/api/v1/delivery-agent/**` | `SCOPE_API` + `ROLE_DELIVERY_AGENT` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | Driver operations |
| `/api/v1/admin/**` | `SCOPE_API` + `ROLE_ADMIN` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | Administrative controls |
| `/actuator/**` (restricted) | `SCOPE_API` + `ROLE_ADMIN` | `GET` | System metrics & diagnostics |

---

## 3. Public Endpoints

### 3.1 Authentication & Registration (`AuthenticationController`, `UserRegistrationController`, `LogoutController`)
* `POST /api/v1/auth/register/customer` — Register a new customer account (`ROLE_CUSTOMER`).
  - Body: `{ "name": "Jane Doe", "email": "jane@example.com", "phoneNumber": "+919876543210", "password": "Password@123" }`
* `POST /api/v1/auth/register/delivery-agent` — Register a new delivery partner account (`ROLE_DELIVERY_AGENT`).
* `POST /api/v1/auth/register/restaurant-owner` — Register a new restaurant owner account (`ROLE_RESTAURANT_OWNER`).
* `POST /api/v1/auth/login` — Authenticate with email and password.
  - Header: `X-Client-Type: web_browser` or `mobile_app` (Required)
  - Body: `{ "email": "jane@example.com", "password": "Password@123" }`
  - Response: `AuthResponse` (`accessToken`, `refreshToken` [for mobile], `expiresIn`, `sessionId`, `tokenType`). Sets `qb_refresh_token` `HttpOnly` cookie for `web_browser`.
  - *Note*: If the user already has 3 active sessions, returns `409 Conflict` with `SessionLimitErrorResponse { "error": "session_limit_exceeded", "challengeToken": "...", "maxSessions": 3 }`.
* `POST /api/v1/auth/refresh-token` — Rotate tokens.
  - Header: `X-Client-Type: web_browser` (reads `qb_refresh_token` cookie) or `mobile_app` (reads `{ "refreshToken": "..." }` JSON body).
* `POST /api/v1/auth/logout` — Terminate current session and expire refresh cookie (Requires `SCOPE_API`).
* `POST /api/v1/auth/logout-all` — Terminate all active sessions across all devices for the current user (Requires `SCOPE_API`).

### 3.2 Public Catalog & Discovery (`RestaurantCatalogController`, `CuisineCatalogController`, `MenuItemCatalogController`, `RestaurantReviewController`)
* `GET /api/v1/cuisines` — List all approved cuisines.
* `GET /api/v1/restaurants` — Pageable search of active and verified restaurants.
  - Params: `page`, `size`, `search`, `cuisineId`.
* `GET /api/v1/restaurants/nearby` — PostGIS geospatial query for nearby restaurants.
  - Params: `latitude` (double), `longitude` (double), `radiusKm` (double, default: `5.0`).
* `GET /api/v1/restaurants/{id}` — Get restaurant details, hours, and images.
* `GET /api/v1/restaurants/{id}/menu-items` — Get active menu items for a restaurant.
  - Params: `category`, `dietaryClassification` (`VEG`, `NON_VEG`, `VEGAN`), `search`.
* `GET /api/v1/restaurants/{id}/reviews` — Get paginated customer reviews for a restaurant.
* `GET /api/v1/restaurants/{id}/reviews/summary` — Aggregate star ratings and review counts.

### 3.3 Payment Webhooks (`PaymentWebhookController`)
* `POST /api/v1/webhooks/payments/razorpay` — Incoming Razorpay event notification.
  - Header: `X-Razorpay-Signature: <hmac_sha256>`
  - Body: Raw Razorpay webhook JSON payload.
* `POST /api/v1/webhooks/payments/stub` — Development stub webhook handler.

---

## 4. Customer Endpoints (`ROLE_CUSTOMER`)

### 4.1 Profile & Address Management (`CustomerProfileController`, `CustomerAddressController`)
* `GET /api/v1/customer/me` — Retrieve customer profile.
* `PUT /api/v1/customer/me` — Update customer profile details.
* `GET /api/v1/customer/me/addresses` — List saved customer delivery addresses.
* `POST /api/v1/customer/me/addresses` — Create a delivery address with GPS coordinates.
  - Body: `{ "addressLine1": "123 Main St", "landmark": "Near Metro", "city": "Bengaluru", "state": "Karnataka", "pincode": "560001", "latitude": 12.9716, "longitude": 77.5946, "isDefault": true }`
* `PUT /api/v1/customer/me/addresses/{id}` — Update delivery address.
* `DELETE /api/v1/customer/me/addresses/{id}` — Delete delivery address.
* `PUT /api/v1/customer/me/addresses/{id}/default` — Set an address as default.

### 4.2 Shopping Cart (`CartController`)
* `GET /api/v1/customer/cart` — Get active shopping cart (24h rolling TTL).
* `POST /api/v1/customer/cart/items` — Add item to cart (enforces single-restaurant rule; throws `409 Conflict` if cart contains items from another restaurant).
  - Body: `{ "menuItemId": "<uuid>", "quantity": 2 }`
* `PUT /api/v1/customer/cart/items/{cartItemId}` — Update item quantity.
  - Body: `{ "quantity": 3 }`
* `DELETE /api/v1/customer/cart/items/{cartItemId}` — Remove item from cart.
* `DELETE /api/v1/customer/cart` — Clear entire cart.
* `GET /api/v1/customer/cart/checkout-preview` — Real-time route distance, delivery fee (base + distance + surge), platform fee, GST, and total preview.
  - Params: `addressId` (UUID, required), `tipAmount` (BigDecimal, optional).

### 4.3 Orders & Payments (`CustomerOrderController`, `PaymentController`)
* `POST /api/v1/customer/orders` — Place order from active cart (5s Redisson cooldown + price-drift check).
  - Body: `{ "addressId": "<uuid>", "paymentMethod": "UPI | CARD | NET_BANKING | WALLET | COD", "tipAmount": 20.00, "expectedTotal": 345.50 }`
  - Returns: `201 Created` with `PaymentResult` (`CodPaymentResult`, `OnlinePaymentResult`, or `StubOnlinePaymentResult`). Throws `409 Conflict` (`PriceChangedException`) if `expectedTotal` differs from freshly computed price.
* `GET /api/v1/customer/orders` — Cursor-paginated order history (`cursor`, `size`).
* `GET /api/v1/customer/orders/{orderId}` — Order details with item breakdown, pricing, and status history.
* `POST /api/v1/customer/orders/{orderId}/retry-payment` — Reconcile prior attempts and re-initiate payment within the 15-minute window.
  - Params: `paymentMethod` (optional override, defaults to `UPI`).
* `DELETE /api/v1/customer/orders/{orderId}/cancel` — Cancel order (allowed while in `AWAITING_PAYMENT`, `PAYMENT_FAILED`, or `PLACED` prior to restaurant acceptance).
* `GET /api/v1/customer/orders/{orderId}/delivery-agent` — Track assigned delivery agent details and coordinates.
* `GET /api/v1/customer/payments/order/{orderId}` — Get payment details, all attempt summaries, and trigger live reconciliation if `PENDING`.
* `POST /api/v1/customer/payments/verify` — Verify online payment HMAC-SHA256 signature and transition order to `PLACED`.
  - Body: `{ "gatewayOrderId": "order_...", "gatewayPaymentId": "pay_...", "gatewaySignature": "..." }`

### 4.4 Reviews (`CustomerReviewController`)
* `POST /api/v1/customer/reviews` — Submit review for a delivered order.
  - Body: `{ "orderId": "<uuid>", "rating": 5, "comment": "Delicious food and prompt delivery!" }`
* `GET /api/v1/customer/reviews` — List all reviews submitted by the customer.
* `GET /api/v1/customer/reviews/{id}` — Get single review details.
* `PUT /api/v1/customer/reviews/{id}` — Update a previously submitted review.
* `DELETE /api/v1/customer/reviews/{id}` — Remove review.

---

## 5. Restaurant Owner Endpoints (`ROLE_RESTAURANT_OWNER`)

### 5.1 Onboarding & Applications (`RestaurantApplicationController`)
* `POST /api/v1/restaurant/applications` — Start a new restaurant partner application.
* `GET /api/v1/restaurant/applications/mine` — View current partner applications.
* `GET /api/v1/restaurant/applications/{id}` — View application details and approval status.
* `PUT /api/v1/restaurant/applications/{id}/details` — Update restaurant business details.
* `PUT /api/v1/restaurant/applications/{id}/address` — Update restaurant physical location.
* `PUT /api/v1/restaurant/applications/{id}/hours` — Define weekly operating hours.
* `POST /api/v1/restaurant/applications/{id}/documents` — Attach FSSAI/GST documents.
* `POST /api/v1/restaurant/applications/{id}/images` — Attach restaurant facade/kitchen photos.
* `POST /api/v1/restaurant/applications/{id}/submit` — Submit application for admin review.
* `POST /api/v1/restaurant/applications/{id}/reopen` — Reopen and edit a rejected application.

### 5.2 Restaurant Management (`RestaurantOwnerController`, `MenuItemOwnerController`, `CuisineOwnerController`)
* `GET /api/v1/restaurant-owner/restaurants` — List owned restaurants.
* `GET /api/v1/restaurant-owner/restaurants/{id}` — Get owned restaurant details.
* `PUT /api/v1/restaurant-owner/restaurants/{id}` — Update basic details and contact info.
* `PUT /api/v1/restaurant-owner/restaurants/{id}/hours` — Update operating schedule.
* `POST /api/v1/restaurant-owner/restaurants/{id}/images` — Add gallery images.
* `DELETE /api/v1/restaurant-owner/restaurants/{id}/images/{imageId}` — Delete gallery image.
* `PATCH /api/v1/restaurant-owner/restaurants/{id}/toggle-closed` — Toggle temporary store closure.
* `POST /api/v1/restaurant-owner/restaurants/{restaurantId}/menu-items` — Create new menu item.
* `PUT /api/v1/restaurant-owner/restaurants/{restaurantId}/menu-items/{itemId}` — Update menu item.
* `DELETE /api/v1/restaurant-owner/restaurants/{restaurantId}/menu-items/{itemId}` — Delete menu item.
* `PATCH /api/v1/restaurant-owner/restaurants/{restaurantId}/menu-items/{itemId}/availability` — Toggle item availability (86-ing an item).
* `POST /api/v1/restaurant-owner/cuisines/requests` — Request a new cuisine category.

### 5.3 Order Fulfillment (`RestaurantOrderController`)
All endpoints require query parameter `?restaurantId=<uuid>`:
* `GET /api/v1/restaurant-owner/orders` — Cursor-paginated restaurant orders (`restaurantId`, optional `status`, `cursor`, `size`).
* `GET /api/v1/restaurant-owner/orders/{orderId}` — Detailed order view (`restaurantId`).
* `POST /api/v1/restaurant-owner/orders/{orderId}/accept` — Accept order within 2-minute deadline (`PLACED` &rarr; `ACCEPTED`, initiates parallel driver dispatch).
* `POST /api/v1/restaurant-owner/orders/{orderId}/decline` — Decline order (`PLACED` &rarr; `DECLINED`, triggers automatic customer refund).
* `POST /api/v1/restaurant-owner/orders/{orderId}/mark-preparing` — Transition status (`ACCEPTED` &rarr; `PREPARING`).
* `POST /api/v1/restaurant-owner/orders/{orderId}/mark-ready` — Mark kitchen preparation complete (`PREPARING` &rarr; `READY_FOR_PICKUP`).
* `GET /api/v1/restaurant-owner/orders/{orderId}/delivery-agent` — View delivery agent assigned to the order.

---

## 6. Delivery Agent Endpoints (`ROLE_DELIVERY_AGENT`)

### 6.1 Agent Status & Location (`DeliveryAgentController`)
* `GET /api/v1/delivery-agent/me` — Agent profile, status, and assigned vehicle.
* `PATCH /api/v1/delivery-agent/availability` — Toggle duty status (`isAvailable: true/false`).
* `POST /api/v1/delivery-agent/location` — Send live GPS heartbeat ping (lat, lng).
  - Updates Redis GEO spatial index with 60s TTL.

### 6.2 Dispatch Offers & Execution (`DeliveryAgentController`)
* `GET /api/v1/delivery-agent/offers` — View active delivery offers (45s acceptance window).
* `POST /api/v1/delivery-agent/offers/{offerId}/accept` — Accept delivery dispatch offer.
* `POST /api/v1/delivery-agent/offers/{offerId}/reject` — Decline offer.
* `POST /api/v1/delivery-agent/orders/{orderId}/pickup` — Confirm order pickup from restaurant (`OUT_FOR_DELIVERY`).
* `POST /api/v1/delivery-agent/orders/{orderId}/deliver` — Confirm successful delivery to customer (`DELIVERED`).

### 6.3 Vehicles & Onboarding (`DeliveryAgentVehicleController`, `DeliveryAgentApplicationController`, `VehicleApplicationController`)
* `GET /api/v1/delivery-agent/vehicles` — List vehicles registered under the agent.
* `PATCH /api/v1/delivery-agent/vehicles/{ownershipId}/select` — Select active delivery vehicle.
* `PATCH /api/v1/delivery-agent/vehicles/{ownershipId}/deactivate` — Deactivate vehicle.
* Full onboarding endpoints under `/api/v1/delivery-agent/applications/**` and `/api/v1/delivery-agent/vehicles/applications/**`.

---

## 7. Admin Endpoints (`ROLE_ADMIN`)

### 7.1 Allotment & Workload Management (`AdminAllotmentController`)
* `GET /api/v1/admin/allotments/my-assignments` — Retrieve pending onboarding applications assigned to current admin.
* `POST /api/v1/admin/allotments/{id}/complete` — Mark assignment completed.

### 7.2 Application Review & Verification
* `GET /api/v1/admin/restaurant-applications` — List applications.
* `POST /api/v1/admin/restaurant-applications/{id}/approve` — Approve restaurant application.
* `POST /api/v1/admin/restaurant-applications/{id}/reject` — Reject with reason.
* Corresponding endpoints exist for Delivery Agent (`AdminDeliveryAgentApplicationController`) and Vehicle applications (`AdminVehicleApplicationController`).

### 7.3 Delivery & System Operations (`AdminDeliveryController`, `AdminOrderController`, `AdminPaymentController`, `AdminUserController`, `AdminReviewManagementController`)
* `GET /api/v1/admin/delivery-agents` — View all delivery agents with current status.
* `PATCH /api/v1/admin/delivery-agents/{id}/suspend` — Suspend a delivery agent.
* `PATCH /api/v1/admin/delivery-agents/{id}/unsuspend` — Reactivate a delivery agent.
* `POST /api/v1/admin/orders/{orderId}/dispatch/retry` — Manually retry dispatch for an exhausted order.
* `POST /api/v1/admin/orders/{orderId}/refund` — Force admin refund for an order.
* `GET /api/v1/admin/payments/order/{orderId}` — Audit payment and gateway transaction IDs.
* `POST /api/v1/admin/users/admins` — Provision new admin user.
* `DELETE /api/v1/admin/reviews/{id}` — Moderate and delete abusive or fraudulent review.

---

## 8. Cross-Cutting Endpoints

### 8.1 File Uploads (`FileUploadController`)
* `POST /api/v1/files/upload-url` — Generate short-lived AWS S3 presigned PUT URL.
  - Requires: `SCOPE_API`
  - Body: `{ "context": "RESTAURANT_IMAGE \| RESTAURANT_DOCUMENT", "applicationId": "<uuid>", "contentType": "image/jpeg", "fileName": "kitchen.jpg" }`
  - Response: `{ "uploadUrl": "https://quickbite-uploads.s3....", "fileUrl": "https://quickbite-uploads.s3.ap-south-1.amazonaws.com/restaurant-applications/..." }`

### 8.2 Notifications (`NotificationController`)
* `GET /api/v1/notifications` — Paginated user notifications.
* `PATCH /api/v1/notifications/{id}/read` — Mark notification as read.
* `PATCH /api/v1/notifications/read-all` — Mark all notifications as read.
* `GET /api/v1/notifications/unread-count` — Count of unread notifications.

### 8.3 Session Challenge Management (`SessionChallengeController`)
* `GET /api/v1/auth/sessions` — List active sessions (Requires `SCOPE_AUTH`).
* `DELETE /api/v1/auth/sessions/{sessionId}` — Terminate remote session to make room for login.
* `POST /api/v1/auth/claim-session` — Claim new session after resolving challenge.
