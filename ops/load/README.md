## Load Testing the Payment Platform with k6

This directory contains a k6 script to generate payment traffic against the platform.

### Running the Test

1. Ensure the platform is deployed and accessible via HTTPS.
2. Export the base URL of your ingress:

```bash
export BASE_URL="https://your-domain-or-ip"
```

3. Run k6:

```bash
k6 run payments-load-test.js
```

During the test, watch:

- HPA scaling in the `dev` namespace.
- Grafana dashboards for request rate, latency, and error rate.

