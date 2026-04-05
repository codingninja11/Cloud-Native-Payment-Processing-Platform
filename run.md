# How to Run the Payment Platform

Step-by-step guidelines for running the Cloud-Native Payment Processing Platform locally and on Azure.

---

## Prerequisites

| Tool | Purpose | Install |
|------|---------|---------|
| **Java 21** | Run microservices | `brew install openjdk@21` |
| **Maven** | Build services | `brew install maven` |
| **Docker** | PostgreSQL, Kafka (optional) | [docker.com](https://docker.com) |
| **Terraform** | Azure infra (for cloud deploy) | `brew install terraform` |
| **Helm** | Deploy to Kubernetes | `brew install helm` |
| **kubectl** | Kubernetes CLI (for AKS) | `brew install kubectl` |

---

## Option 1: Quick Start (Payment + Fraud UI)

**No Docker required.** Payment uses H2; Fraud Rules tab needs fraud-service.

```bash
# Terminal 1 - Payment service + Web UI
cd services/payment-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2 - Fraud service (for Fraud Rules tab)
cd services/fraud-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

- **Web UI:** http://localhost:8080  
- **Payment API:** http://localhost:8080/api/payments  
- **Fraud Rules:** http://localhost:8082/api/fraud/rules (when fraud-service runs)  

### Test the API

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-123" \
  -d '{"orderId":"ORD-1","userId":"USER-1","amount":99.99,"currency":"USD"}'
```

---

## Localhost: Supabase + `.env` (order + payment)

Use this when your database is **Supabase Postgres** and credentials live in a **`.env`** file at the **repository root** (same folder as `ops/load-env.sh`).

**Important:** Spring Boot does **not** read `.env` files. You must **`source ops/load-env.sh`** in **each** terminal before `mvn spring-boot:run`, or copy `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE`, etc. into your IDE run configuration.

### 1. Kafka (required for order-service with `supabase` profile)

Order-service still connects to Kafka consumers on `localhost:9092`. Start a broker first:

```bash
docker run -d --name kafka -p 9092:9092 apache/kafka:latest
```

If Kafka is already running, skip this step.

### 2. Terminal — order-service (port 8081)

From the **repository root** (directory containing `ops/` and `.env`):

```bash
source ops/load-env.sh
cd services/order-service && mvn spring-boot:run
```

### 3. Terminal — payment-service (port 8080, Web UI)

Open a **new** terminal, again from the **repository root**:

```bash
source ops/load-env.sh
cd services/payment-service && mvn spring-boot:run
```

### 4. Optional — fraud-service (port 8082)

```bash
source ops/load-env.sh
cd services/fraud-service && mvn spring-boot:run
```

### 5. Verify services and database target

```bash
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8080/actuator/health
```

In each service’s startup logs, confirm **`Datasource URL`** points at Supabase (`db.<project-ref>.supabase.co`), not `jdbc:postgresql://localhost:5432/paymentdb`.

### 6. Create an order, then a payment

```bash
# Create order (returns orderId in JSON)
curl -s -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"amount":12.34,"userId":"local-test","currency":"USD"}'

# Create payment (replace ORD-... with the orderId from above)
curl -s -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: local-test-001" \
  -d '{"orderId":"ORD-REPLACE_ME","userId":"local-test","amount":12.34,"currency":"USD"}'
```

- **Web UI:** http://localhost:8080  

---

## Option 2: All Services Locally (with Docker)

Run the full event-driven flow: Payment → Kafka → Fraud → Order.

### Step 1: Start PostgreSQL and Kafka

```bash
# PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=paymentdb \
  -p 5432:5432 \
  postgres:16

# Kafka (single broker)
docker run -d --name kafka \
  -p 9092:9092 \
  apache/kafka:latest
```

### Step 2: Create platform secret (for Kubernetes) or set env vars

For local runs, set these environment variables before starting each service.

**Either** export manually:

```bash
export DB_URL=jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres?sslmode=require
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

**Or** put the same keys in a **`.env`** at the repo root and run **`source ops/load-env.sh`** in each terminal before `mvn` (see **Localhost: Supabase + `.env`** above).

### Step 3: Run all services

Open **3 terminals**. From the **repository root**, load env if you use `.env`, then:

```bash
# Terminal 1 - Payment Service (port 8080)
source ops/load-env.sh   # omit if you already exported DB_* / KAFKA_* manually
cd services/payment-service
mvn spring-boot:run

# Terminal 2 - Order Service (port 8081)
source ops/load-env.sh
cd services/order-service
mvn spring-boot:run

# Terminal 3 - Fraud Service (port 8082)
source ops/load-env.sh
cd services/fraud-service
mvn spring-boot:run
```

### Step 4: Access the Web UI

Open **http://localhost:8080** in your browser. The UI lets you:

- Create and lookup payments  
- Create and lookup orders  
- View fraud rules  

---

## Option 3: Build (No Run)

Verify the project compiles:

```bash
# Build all services
cd services/payment-service && mvn -B package -DskipTests
cd ../order-service && mvn -B package -DskipTests
cd ../fraud-service && mvn -B package -DskipTests
```

---

## Option 4: Deploy to Kubernetes (images from GHCR)

Use **any** cluster (minikube, kind, k3d, cloud vendor, etc.). Images are built on push to `main` by `.github/workflows/publish-images.yml` and land in **GitHub Container Registry**:

`ghcr.io/<your-github-owner-lowercase>/<payment-service|order-service|fraud-service>:dev`

### 1. Cluster and namespace

```bash
# Example: minikube
minikube start

kubectl apply -f infra/k8s/namespaces.yaml
```

### 2. Pull images from GHCR

- If packages are **public**, no extra step.
- If **private**, create a pull secret and ensure Pods use it (you may need to extend the Helm chart with `imagePullSecrets`, or temporarily use `kubectl patch` on the Deployment after install).

```bash
# GitHub → Settings → Developer settings → Fine-grained or classic PAT with read:packages
kubectl create secret docker-registry ghcr-credentials \
  --namespace=dev \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_TOKEN
# Then wire this secret into your chart or patch deployments to reference it.
```

### 3. Platform secret (`platform-config`)

Services expect a Secret named `platform-config` in namespace `dev` (see each service `chart/values.yaml`: `configSecret`).

- **Still using Azure PostgreSQL + Event Hubs?** Use `./ops/create-platform-secret.sh` with your connection details (same as before).
- **Self-managed DB/Kafka:** create the same keys your apps need (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, plus any Kafka SASL vars your profile requires—compare with `ops/create-platform-secret.sh`).

### 4. Install ingress (optional)

If you use ingress routes from the charts, install an ingress controller (for example the manifest under `infra/k8s/` or the official NGINX Ingress Helm chart), then set `base-service.ingress.hosts` via `--set` or by editing each service `values.yaml` (`REPLACE_ME_HOST`).

### 5. Helm install

Replace `YOUR_GITHUB_OWNER` with your GitHub user or org name **in lowercase**.

```bash
export OWNER=your-github-owner   # lowercase

helm dependency update services/payment-service/chart
helm dependency update services/order-service/chart
helm dependency update services/fraud-service/chart

helm upgrade --install payment-service services/payment-service/chart -n dev \
  --set base-service.image.repository=ghcr.io/$OWNER/payment-service \
  --set base-service.image.tag=dev

helm upgrade --install order-service services/order-service/chart -n dev \
  --set base-service.image.repository=ghcr.io/$OWNER/order-service \
  --set base-service.image.tag=dev

helm upgrade --install fraud-service services/fraud-service/chart -n dev \
  --set base-service.image.repository=ghcr.io/$OWNER/fraud-service \
  --set base-service.image.tag=dev
```

### 6. Verify

```bash
kubectl get pods -n dev
kubectl get ingress -n dev   # if ingress enabled
```

**Local images (no GHCR):** from the **repo root**, `docker build -f services/payment-service/Dockerfile -t ghcr.io/$OWNER/payment-service:dev .` (same pattern for `order-service` / `fraud-service`), then `docker push` after `docker login ghcr.io`, or load into kind/minikube (`minikube image load`, `kind load docker-image`).

---

## Option 5: Deploy to Azure (AKS)

### 1. Provision infrastructure

```bash
cd infra/terraform/envs/dev
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
# Edit main.tf backend block for remote state

terraform init
terraform plan
terraform apply
```

### 2. Create platform secret

```bash
# From project root
./ops/create-platform-secret.sh \
  "$(terraform -chdir=infra/terraform/envs/dev output -raw postgresql_fqdn)" \
  "$(terraform -chdir=infra/terraform/envs/dev output -raw event_hubs_namespace)" \
  "$(terraform -chdir=infra/terraform/envs/dev output -raw event_hubs_connection_string)" \
  cnppadmin "YOUR_DB_PASSWORD"
```

### 3. Deploy services

```bash
# Get AKS credentials
az aks get-credentials --resource-group <rg-name> --name <aks-name>

# Update Helm dependencies
helm dependency update services/payment-service/chart
helm dependency update services/order-service/chart
helm dependency update services/fraud-service/chart

# Deploy (replace <acr-name> with your ACR)
helm upgrade --install payment-service services/payment-service/chart -n dev \
  --set base-service.image.repository=<acr-name>.azurecr.io/payment-service \
  --set base-service.image.tag=dev

helm upgrade --install order-service services/order-service/chart -n dev \
  --set base-service.image.repository=<acr-name>.azurecr.io/order-service \
  --set base-service.image.tag=dev

helm upgrade --install fraud-service services/fraud-service/chart -n dev \
  --set base-service.image.repository=<acr-name>.azurecr.io/fraud-service \
  --set base-service.image.tag=dev
```

### 4. Access via Ingress

```bash
kubectl get ingress -n dev
# Use the HOST or ADDRESS to access the UI and APIs
```

---

## Verification Checklist

| Check | Command |
|-------|---------|
| Payment service up | `curl http://localhost:8080/actuator/health` |
| Order service up | `curl http://localhost:8081/actuator/health` |
| Fraud service up | `curl http://localhost:8082/actuator/health` |
| Web UI | Open http://localhost:8080 in browser |
| Create payment | Use UI or `curl -X POST ...` (see Option 1) |

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Connection refused` to PostgreSQL | Start PostgreSQL (Docker or local) |
| `Connection refused` to Kafka | Start Kafka or use `local` profile for payment-service only |
| Port already in use | Stop other processes on 8080, 8081, 8082 or change ports in `application.yaml` |
| `mvn: command not found` | Install Maven: `brew install maven` |
| CORS errors in UI | Ensure order-service and fraud-service are running (they have CORS enabled for localhost:8080) |
| API works but rows missing in Supabase | Spring does not read `.env`; run `source ops/load-env.sh` in the same shell before `mvn`, or set env vars in the IDE. Check logs for `Datasource URL` — it must show your Supabase host. |
| `source ops/load-env.sh` then wrong directory | Always `cd` to **repository root** first (where `ops/load-env.sh` lives), then `source ops/load-env.sh`, then `cd services/...`. |

---

## More Details

- **Architecture:** `docs/architecture.md`  
- **API contracts:** `docs/contracts.md`  
- **Full Azure walkthrough:** `docs/walkthrough.md`  
- **Load testing:** `ops/load/README.md`  
