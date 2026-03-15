## Cloud-Native Scalable Payment Processing Platform (Azure, AKS, DevOps)

This repository contains a **cloud-native, event-driven payment processing platform** designed for a banking/fintech-style environment. It is optimized as a **portfolio project for a 3–5 year DevOps engineer**, showcasing:

- **Azure-based cloud infrastructure** provisioned with Terraform.
- **Kubernetes (AKS)** for microservices orchestration.
- **Kafka-compatible messaging (Azure Event Hubs)** for event-driven payments.
- **Production-grade DevOps practices**: CI/CD with GitHub Actions, observability, autoscaling, and security controls.

### Core Use Case

The platform simulates a realistic payment flow:

1. A client submits a payment request via a secure API endpoint.
2. The **Payment Service** validates and records the payment, then publishes an event.
3. The **Fraud Service** evaluates the payment and emits a risk decision.
4. The **Order Service** updates order state based on payment and fraud events.
5. Metrics, logs, and traces are collected at each step for full observability.

### Web UI

A simple web UI is included for creating and viewing payments, orders, and fraud rules. When the payment service runs, open **http://localhost:8080** in your browser.

### Key Microservices

- **Payment Service**: Handles payment initiation, idempotency, and event publication.
- **Order Service**: Manages order lifecycle and reacts to payment/fraud outcomes.
- **Fraud Service**: Applies simple rules-based fraud checks on incoming payments.

Each service is:

- Containerized with **Docker**.
- Deployed to **AKS** via **Helm charts**.
- Integrated with **Azure Event Hubs (Kafka API)** and **PostgreSQL**.
- Exposes **health** (`/healthz`, `/readyz`) and **Prometheus metrics** endpoints.

### High-Level Architecture

The architecture follows the plan in `docs/` and can be summarized as:

- **Ingress/API Gateway layer**:
  - NGINX Ingress Controller on AKS (optionally fronted by Azure Application Gateway/WAF).
  - Routes `/payments`, `/orders`, etc. to backend services.
- **Service layer (microservices)**:
  - `payment-service`, `order-service`, `fraud-service` running as separate Deployments.
- **Data & Messaging**:
  - Azure Database for PostgreSQL for transactional data.
  - Azure Event Hubs (Kafka-compatible) for payment/fraud/order events.
- **Observability & Logging**:
  - Prometheus + Grafana for metrics and dashboards.
  - Fluent Bit/AKS integration with Log Analytics for centralized logs.
- **DevOps & Security**:
  - Terraform for infrastructure as code.
  - GitHub Actions for CI/CD (build, scan, deploy).
  - RBAC, NetworkPolicies, Key Vault, and TLS for security and compliance.

### Repository Structure

Planned structure (files/folders will be added progressively as you follow the guide):

- `infra/terraform/` – Azure infrastructure (VNet, AKS, PostgreSQL, Event Hubs, Key Vault, Log Analytics).
- `infra/k8s/` – Kubernetes manifests / Helm charts for platform components (ingress, observability, logging, autoscaling).
- `services/payment-service/` – Payment microservice source, Dockerfile, and Helm chart.
- `services/order-service/` – Order microservice source, Dockerfile, and Helm chart.
- `services/fraud-service/` – Fraud microservice source, Dockerfile, and Helm chart.
- `ops/` – Load testing scripts (k6/JMeter), helper scripts, DB migrations.
- `.github/workflows/` – GitHub Actions CI/CD pipelines.
- `docs/` – Architecture diagrams, API/event contracts, and walkthrough guides.

### Getting Started (High Level)

1. **Prerequisites**
   - Azure subscription (Free Tier is sufficient for a small dev/demo deployment).
   - `az` CLI, `kubectl`, `helm`, `terraform`, and `docker` installed locally.
   - GitHub repository connected to this codebase.

2. **Provision Azure Infrastructure**
   - Configure Terraform variables for Azure subscription, resource group, and environment.
   - From `infra/terraform/envs/dev`, run:
     - `terraform init`
     - `terraform plan`
     - `terraform apply`

3. **Deploy Platform Components to AKS**
   - Configure `kubectl` to point to the AKS cluster created by Terraform.
   - From `infra/k8s`, install:
     - Ingress controller
     - Observability stack (Prometheus + Grafana)
     - Logging components

4. **Build & Deploy Microservices**
   - Use GitHub Actions (or local `docker build` and `helm upgrade --install`) to:
     - Build Docker images and push to Azure Container Registry (ACR).
     - Deploy Helm charts for `payment-service`, `order-service`, and `fraud-service`.

5. **Run Load Tests & Observe Autoscaling**
   - Use k6/JMeter configs in `ops/load` to generate payment traffic.
   - Watch Grafana dashboards to see:
     - Throughput and latency.
     - Error rates.
     - HPA/KEDA-driven pod scaling.

### Documentation

Additional details and implementation steps are documented in:

- **`HOW_TO_RUN.md`** – Step-by-step run guidelines (local, Docker, Azure).
- `docs/architecture.md` – Architecture diagrams and component descriptions.
- `docs/contracts.md` – HTTP and event contracts for services and Kafka topics.
- `docs/walkthrough.md` – Step-by-step guide on deploying the platform, running tests, and showcasing it as a portfolio project.

This project is intentionally scoped so you can **iterate in layers**: you can start with a minimal, local-only deployment and progressively enable Azure infrastructure, observability, autoscaling, and security to demonstrate your DevOps skillset.

