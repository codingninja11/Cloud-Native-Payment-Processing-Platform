## Resilience, Idempotency, and Disaster Recovery

This document summarizes how the payment platform handles failures, avoids double-charging, and recovers from disasters.

### Idempotency & Message Processing

- **HTTP Idempotency**:
  - Clients send an `Idempotency-Key` header with `POST /api/payments`.
  - The Payment service can use this key (stored in PostgreSQL or Redis) to ensure repeated requests do not create duplicate payments.
- **Kafka/Event Hubs Semantics**:
  - Consumers in Order and Fraud services should be built as **idempotent**:
    - Use `paymentId` / `orderId` as natural idempotency keys.
    - Safely handle retries by checking current state before updating.

### Retries and Backoff

- **Outbound Calls**:
  - When calling external systems (e.g., card processors), implement:
    - Bounded retries with exponential backoff.
    - Circuit breaker pattern to avoid cascading failures.
- **Consumer Retries**:
  - On transient errors (e.g., temporary DB outage), retry event processing a few times before sending messages to a dead-letter topic.

### Dead-Letter Queues (DLQ)

- Messages that cannot be processed after retries are sent to the `dead-letter` Event Hub (topic).
- Each dead-letter message includes:
  - Original topic and key.
  - Original payload.
  - Error message and retry count.
- Operators can replay or investigate these messages using separate tooling.

### High Availability

- **Kubernetes**:
  - Multiple replicas per service (HPA scales pods horizontally).
  - PodDisruptionBudgets (can be added) to avoid full downtime during maintenance.
- **Database**:
  - Azure Database for PostgreSQL provides zone redundancy options and automated backups.
- **Event Hubs**:
  - Managed platform with built-in redundancy and partitioning.

### Backups and Disaster Recovery

- **PostgreSQL**:
  - Automatic backups with a retention period (e.g., 7+ days).
  - Point-in-time restore to a new server in case of catastrophic corruption.
- **Terraform State & Config**:
  - Terraform state stored in Azure Storage with redundancy.
  - Configuration and Helm values stored in Git and can be re-applied to rebuild environments.

### RPO/RTO Considerations

- Example targets you can document:
  - **RPO (Recovery Point Objective)**: 5–15 minutes, depending on backup frequency and Event Hubs retention.
  - **RTO (Recovery Time Objective)**: 30–60 minutes to restore DB and redeploy core services via Terraform + Helm.

