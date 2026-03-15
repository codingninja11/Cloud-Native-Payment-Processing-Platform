## Walkthrough: Running the Cloud-Native Payment Platform on Azure

This guide walks you (or a reviewer) through deploying and demonstrating the **Cloud-Native Scalable Payment Processing Platform** on Azure.

> Note: This is a **demo-friendly** setup designed to work with Azure Free Tier–style limits. You can scale up resources later for more realistic load tests.

---

### 1. Prerequisites

- Azure subscription and permissions to create:
  - Resource groups, AKS, Azure Database for PostgreSQL, Event Hubs, Key Vault, Log Analytics.
- Local tools:
  - `az` CLI
  - `kubectl`
  - `helm`
  - `terraform`
  - `docker`
  - `k6` (or Docker for running k6 in a container)
- A GitHub repository containing this codebase and permissions to configure GitHub Actions.

---

### 2. Provision Infrastructure with Terraform

1. **Log in to Azure**:

   ```bash
   az login
   az account set --subscription "<your-subscription-id>"
   ```

2. **Configure Terraform backend and variables** in `infra/terraform/envs/dev`:

   - Edit `main.tf` to set the backend block (resource group, storage account, container for remote state).
   - Copy `terraform.tfvars.example` to `terraform.tfvars` and fill in:
     - `resource_group_name`, `location`
     - `db_admin_username`, `db_admin_password`
     - `key_vault_name`, `acr_name` (ACR name must be globally unique)

3. **Initialize and apply**:

   ```bash
   cd infra/terraform/envs/dev
   terraform init
   terraform plan
   terraform apply
   ```

4. **Export AKS credentials**:

   ```bash
   az aks get-credentials \
     --resource-group <rg-name> \
     --name <aks-cluster-name> \
     --overwrite-existing
   ```

At this point, you should be able to run:

```bash
kubectl get nodes
```

and see your AKS nodes.

---

### 3. Deploy Platform Components to AKS

1. **Create namespaces and base platform resources** (if using Helm charts in `infra/k8s`):

   ```bash
   cd infra/k8s
   # Example if using a platform chart
   helm upgrade --install platform ./charts/platform -n ingress --create-namespace
   helm upgrade --install observability ./charts/observability -n observability --create-namespace
   ```

2. **Verify components**:

   ```bash
   kubectl get pods -n ingress
   kubectl get pods -n observability
   ```

3. **Set up port-forwarding for Grafana (if needed)**:

   ```bash
   kubectl port-forward svc/grafana -n observability 3000:80
   ```

   Then open `http://localhost:3000` in your browser to access Grafana.

---

### 4. Create Platform Secret (DB & Event Hubs)

Before deploying microservices, create the `platform-config` secret with database and Event Hubs connection details. Run from the project root:

```bash
# Get Terraform outputs
cd infra/terraform/envs/dev
PG_FQDN=$(terraform output -raw postgresql_fqdn)
EH_NS=$(terraform output -raw event_hubs_namespace)
EH_CS=$(terraform output -raw event_hubs_connection_string)

# Create the secret (replace DB_PASSWORD with your db_admin_password)
cd ../../..
./ops/create-platform-secret.sh "$PG_FQDN" "$EH_NS" "$EH_CS" cnppadmin "YOUR_DB_PASSWORD"
```

> **Note:** Ensure the `dev` namespace exists (`kubectl get ns dev`). If not, apply `infra/k8s/namespaces.yaml` first.

---

### 5. Build and Deploy Microservices

You can either:

- Use **GitHub Actions** (recommended) to build images and deploy automatically, or
- Build and deploy locally for quick iteration.

#### 5.1 Local Build & Push (ACR)

1. Retrieve ACR name from Terraform outputs or Azure portal.
2. Log in and build images:

   ```bash
   az acr login --name <acr-name>

   cd services/payment-service
   docker build -t <acr-name>.azurecr.io/payment-service:dev .
   docker push <acr-name>.azurecr.io/payment-service:dev

   # Repeat for order-service and fraud-service
   ```

#### 5.2 Deploy via Helm

From the project root (after creating the platform-config secret):

```bash
helm dependency update services/payment-service/chart
helm dependency update services/order-service/chart
helm dependency update services/fraud-service/chart

helm upgrade --install payment-service ./services/payment-service/chart -n dev \
  --set base-service.image.repository=<acr-name>.azurecr.io/payment-service \
  --set base-service.image.tag=dev

helm upgrade --install order-service ./services/order-service/chart -n dev \
  --set base-service.image.repository=<acr-name>.azurecr.io/order-service \
  --set base-service.image.tag=dev

helm upgrade --install fraud-service ./services/fraud-service/chart -n dev \
  --set base-service.image.repository=<acr-name>.azurecr.io/fraud-service \
  --set base-service.image.tag=dev
```

Verify that pods are running:

```bash
kubectl get pods -n dev
```

---

### 6. Accessing the API

1. Get the external IP/hostname from the Ingress or Application Gateway:

   ```bash
   kubectl get ingress -n dev
   ```

2. Test the Payment API:

   ```bash
   curl -X POST "https://<your-host>/api/payments" \
     -H "Content-Type: application/json" \
     -H "Idempotency-Key: test-key-1" \
     -d '{
       "orderId": "ORD-1",
       "userId": "USER-1",
       "amount": 10.5,
       "currency": "USD",
       "paymentMethod": "CARD",
       "cardLast4": "4242"
     }'
   ```

---

### 7. Load Testing with k6

1. Navigate to `ops/load` and review the k6 script (e.g., `payments-load-test.js`).
2. Run the load test:

   ```bash
   cd ops/load
   k6 run payments-load-test.js
   ```

   Or using Docker:

   ```bash
   docker run --rm -i loadimpact/k6 run - < payments-load-test.js
   ```

3. While the test is running:
   - Watch HPA scaling:

     ```bash
     kubectl get hpa -n dev -w
     ```

   - Open Grafana and observe dashboards for:
     - Request rate, latency, error rate.
     - Pod counts and resource usage.

---

### 7. CI/CD with GitHub Actions (Overview)

- On pull requests:
  - Build and test services.
  - Run linters and security scans.
- On merges to `main`:
  - Build and push images to ACR.
  - Deploy updated Helm releases to AKS.

GitHub Actions workflow files live under `.github/workflows/` and are documented inline.

---

### 8. What to Show in a Portfolio Review

When walking someone through this project, highlight:

- **Architecture diagrams** in `docs/architecture.md`.
- **API and event contracts** in `docs/contracts.md`.
- **Infrastructure as Code** in `infra/terraform`.
- **Kubernetes and Helm** in `infra/k8s` and `services/*`.
- **Observability**: Grafana dashboards, logs in Log Analytics.
- **Autoscaling**: HPA/KEDA behavior under k6 load.
- **Security**: RBAC, NetworkPolicies, Key Vault integration, TLS at the ingress.

This end-to-end story demonstrates that you can design, provision, operate, and observe a production-style, cloud-native payment system on Azure.

