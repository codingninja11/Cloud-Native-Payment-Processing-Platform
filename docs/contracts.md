## API and Event Contracts

This document defines the **HTTP APIs** and **Kafka-style event contracts** used by the Payment, Order, and Fraud services.

The goal is to keep services **loosely coupled** while ensuring **clear, versioned interfaces**.

---

### Payment Service HTTP API

**Base path**: `/api/payments`

#### `POST /api/payments`

Initiate a new payment.

- **Headers**
  - `Idempotency-Key` (required): Unique string used to de-duplicate requests.
  - `Authorization`: Bearer token (for real systems; can be mocked here).

- **Request body (JSON)**

```json
{
  "orderId": "ORD-123456",
  "userId": "USER-789",
  "amount": 100.50,
  "currency": "USD",
  "paymentMethod": "CARD",
  "cardLast4": "4242"
}
```

- **Response (201 Created)**

```json
{
  "paymentId": "PAY-abc123",
  "orderId": "ORD-123456",
  "status": "PENDING",
  "amount": 100.50,
  "currency": "USD",
  "createdAt": "2026-03-12T10:00:00Z"
}
```

#### `GET /api/payments/{paymentId}`

Retrieve payment status and details.

---

### Order Service HTTP API

**Base path**: `/api/orders`

#### `POST /api/orders`

Create a new order (simplified for this project).

```json
{
  "orderId": "ORD-123456",
  "userId": "USER-789",
  "amount": 100.50,
  "currency": "USD"
}
```

#### `GET /api/orders/{orderId}`

Returns order status including payment and fraud status.

---

### Fraud Service HTTP API (Optional)

The Fraud service is primarily event-driven, but you can optionally expose admin APIs such as:

- `GET /api/fraud/rules`
- `POST /api/fraud/rules`

---

## Event Contracts (Kafka / Event Hubs)

All events are published to **Azure Event Hubs** in **Kafka-compatible mode**. For the project, we use a few key topics:

- `payments` – Payment lifecycle events.
- `fraud-results` – Fraud decision events.
- `orders` – Order lifecycle events (optional).
- `dead-letter` – Failed messages that could not be processed.

### `payment.initiated` Event

**Topic**: `payments`  
**Key**: `paymentId`  
**Type**: `payment.initiated` (in header or payload field)

```json
{
  "eventType": "payment.initiated",
  "eventVersion": "v1",
  "paymentId": "PAY-abc123",
  "orderId": "ORD-123456",
  "userId": "USER-789",
  "amount": 100.50,
  "currency": "USD",
  "paymentMethod": "CARD",
  "createdAt": "2026-03-12T10:00:00Z",
  "idempotencyKey": "abc123-idem-key"
}
```

### `payment.authorized` / `payment.failed` Events

**Topic**: `payments`

```json
{
  "eventType": "payment.authorized",
  "eventVersion": "v1",
  "paymentId": "PAY-abc123",
  "orderId": "ORD-123456",
  "authCode": "AUTH-9999",
  "approvedAmount": 100.50,
  "currency": "USD",
  "processedAt": "2026-03-12T10:00:05Z"
}
```

```json
{
  "eventType": "payment.failed",
  "eventVersion": "v1",
  "paymentId": "PAY-abc123",
  "orderId": "ORD-123456",
  "failureCode": "CARD_DECLINED",
  "failureReason": "Insufficient funds",
  "processedAt": "2026-03-12T10:00:06Z"
}
```

### `fraud.result` Event

**Topic**: `fraud-results`  
**Key**: `paymentId`

```json
{
  "eventType": "fraud.result",
  "eventVersion": "v1",
  "paymentId": "PAY-abc123",
  "orderId": "ORD-123456",
  "userId": "USER-789",
  "score": 0.23,
  "decision": "APPROVED",
  "reason": "Below risk threshold",
  "evaluatedAt": "2026-03-12T10:00:02Z"
}
```

`decision` values could include:

- `APPROVED`
- `REVIEW`
- `DECLINED`

### `order.updated` Event (Optional)

**Topic**: `orders`

```json
{
  "eventType": "order.updated",
  "eventVersion": "v1",
  "orderId": "ORD-123456",
  "userId": "USER-789",
  "status": "PAID",
  "paymentId": "PAY-abc123",
  "fraudDecision": "APPROVED",
  "updatedAt": "2026-03-12T10:00:06Z"
}
```

---

## Error Handling & Dead-Lettering

When consumers fail to process a message after retries:

- The message should be sent to a `dead-letter` topic with metadata:

```json
{
  "originalTopic": "payments",
  "originalKey": "PAY-abc123",
  "payload": { },
  "errorMessage": "Deserialization error",
  "serviceName": "order-service",
  "failedAt": "2026-03-12T10:05:00Z",
  "retryCount": 5
}
```

This pattern lets you demonstrate:

- **Resilience** (no data loss, eventual processing).
- **Operational visibility** into failed events.

