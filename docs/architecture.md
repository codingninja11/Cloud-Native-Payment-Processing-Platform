## Architecture Overview

This document describes the architecture of the **Cloud-Native Scalable Payment Processing Platform** deployed on **Azure AKS** with **Kafka-compatible messaging (Azure Event Hubs)** and **PostgreSQL**.

The goal is to demonstrate **modern DevOps and cloud-native practices** for a banking/fintech-style payment system.

### Component Diagram

```mermaid
flowchart LR
  client[Client Apps]
  azureDns[Azure_DNS]
  appGw[Azure_Application_Gateway/WAF]
  ingress[NGINX_Ingress_Controller]

  subgraph platformCluster[AKS Cluster]
    subgraph ingressNs[ingress_namespace]
      ingress
    end

    subgraph appNs[app_namespace]
      paymentSvc[Payment_Service]
      orderSvc[Order_Service]
      fraudSvc[Fraud_Service]
    end

    subgraph observabilityNs[observability_namespace]
      prometheus[Prometheus]
      grafana[Grafana]
      logging[Fluent_Bit/Log_Collector]
    end
  end

  db[(Azure_Database_for_PostgreSQL)]
  eventHubs[Azure_Event_Hubs_(Kafka_API)]
  keyVault[Azure_Key_Vault]
  logAnalytics[Azure_Log_Analytics]

  client --> azureDns --> appGw --> ingress
  ingress --> paymentSvc
  ingress --> orderSvc

  paymentSvc -->|write/read| db
  orderSvc -->|write/read| db

  paymentSvc -->|\"produce payment.initiated\"| eventHubs
  fraudSvc -->|\"consume payment.initiated\"| eventHubs
  fraudSvc -->|\"produce fraud.result\"| eventHubs
  orderSvc -->|\"consume payment/fraud events\"| eventHubs

  paymentSvc --> prometheus
  orderSvc --> prometheus
  fraudSvc --> prometheus
  prometheus --> grafana

  logging --> logAnalytics
  paymentSvc --> logging
  orderSvc --> logging
  fraudSvc --> logging
  ingress --> logging

  paymentSvc -->|\"secrets (DB, Kafka, JWT)\"| keyVault
  orderSvc -->|\"secrets (DB, Kafka)\"| keyVault
  fraudSvc -->|\"secrets (Kafka)\"| keyVault
```

### Request & Event Flow

1. **API Request**
   - External clients call HTTPS endpoints (e.g., `POST /api/payments`) via:
     - Azure DNS → Application Gateway/WAF → NGINX Ingress → `payment-service`.

2. **Payment Processing**
   - `payment-service`:
     - Validates the request payload and idempotency key.
     - Persists a `payment` record to PostgreSQL.
     - Publishes a `payment.initiated` event to the `payments` topic on Azure Event Hubs (Kafka API).

3. **Fraud Evaluation**
   - `fraud-service` consumes `payment.initiated`:
     - Runs rules-based fraud checks (e.g., amount thresholds, blacklisted users).
     - Emits `fraud.result` events indicating `APPROVED` or `FLAGGED`.

4. **Order Update**
   - `order-service` consumes relevant events:
     - Updates the `order` record in PostgreSQL with the latest payment and fraud status.
     - Optionally emits `order.updated` events.

5. **Observability**
   - Each service exposes Prometheus metrics (RPS, latency, errors, Kafka lag).
   - Logs are sent to Log Analytics (and optionally Elasticsearch) via Fluent Bit.
   - Dashboards in Grafana visualize services, Kafka, and AKS cluster health.

### Kubernetes Logical Layout

- **Namespaces**
  - `ingress` – NGINX Ingress Controller and related resources.
  - `dev` (or `app`) – Application microservices (payment, order, fraud).
  - `observability` – Prometheus, Grafana, log collectors.

- **Core K8s Resources**
  - `Deployment` – One per microservice, plus observability components.
  - `Service` – ClusterIP services for internal communication.
  - `Ingress` – Routes external traffic to services.
  - `HorizontalPodAutoscaler (HPA)` – Scales pods based on CPU/Memory.
  - `KEDA ScaledObject` (optional) – Scales based on Event Hubs/Kafka lag.
  - `ConfigMap` & `Secret` – Configuration and sensitive values.
  - `NetworkPolicy` – Limits traffic between namespaces and services.

### Infrastructure Overview (Terraform)

Terraform provisions:

- **Core Infrastructure**
  - Resource group(s).
  - Virtual Network and subnets (AKS, database, and management subnets).
  - AKS cluster with node pools sized for dev/demo.

- **Data & Messaging**
  - Azure Database for PostgreSQL flexible server (basic tier).
  - Azure Event Hubs namespace with Kafka-compatible event hubs for `payments`, `orders`, `fraud-results`, etc.

- **Security & Secrets**
  - Azure Key Vault for secrets (DB credentials, Kafka connection strings, JWT keys).
  - Managed identities for AKS to access Key Vault.

- **Observability**
  - Log Analytics workspace connected to AKS.
  - (Optionally) alert rules for key metrics.

### SLOs & Non-Functional Requirements

Example non-functional goals you can tune and measure:

- **Availability**: Target 99.5%+ for the dev/demo environment.
- **Performance**:
  - p95 latency for payment initiation < 300–500 ms under nominal load.
  - Stable error rate < 1–2% during load tests.
- **Scalability**:
  - Demonstrate horizontal scaling via HPA/KEDA when RPS or Kafka lag increases.
- **Security**:
  - All external traffic via HTTPS.
  - Secrets never stored in plain text in manifests or code.
  - RBAC and NetworkPolicies enforced at the cluster level.

