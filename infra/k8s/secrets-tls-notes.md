## Secrets and TLS Patterns (Azure AKS)

- **Secrets**:
  - Use Azure Key Vault to store DB credentials, Event Hubs connection strings, and JWT keys.
  - Mount secrets into pods via the Azure Key Vault CSI driver or sync them into Kubernetes `Secret` objects.
- **TLS**:
  - Terminate TLS either at:
    - Azure Application Gateway in front of AKS, or
    - NGINX Ingress with cert-manager issuing certificates.
  - All external traffic should use HTTPS; internal service-to-service traffic can use mTLS if desired.

