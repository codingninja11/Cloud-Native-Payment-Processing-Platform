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

For local runs, set these environment variables before starting each service:

```bash
export DB_URL=jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres?sslmode=require
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Step 3: Run all services

Open **3 terminals** and run:

```bash
# Terminal 1 - Payment Service (port 8080)
cd services/payment-service
mvn spring-boot:run

# Terminal 2 - Order Service (port 8081)
cd services/order-service
mvn spring-boot:run

# Terminal 3 - Fraud Service (port 8082)
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

---

## More Details

- **Architecture:** `docs/architecture.md`  
- **API contracts:** `docs/contracts.md`  
- **Full Azure walkthrough:** `docs/walkthrough.md`  
- **Load testing:** `ops/load/README.md`  
