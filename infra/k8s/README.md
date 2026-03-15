## Kubernetes Platform Layer

This directory contains **Kubernetes manifests and Helm-ready YAML** for the shared platform components:

- Namespaces (`dev`, `ingress`, `observability`)
- NGINX Ingress Controller
- Basic observability stack (Prometheus & Grafana via kube-prometheus-stack values)
- Logging (Fluent Bit DaemonSet to Azure Log Analytics)

These files are intended as **reference manifests** you can apply directly or convert into Helm charts.

