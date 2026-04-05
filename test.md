Demo script for the interview (short and clear)
1. Open the UI

Go to http://localhost:8080.
Say: “This is a small cloud‑native payment platform: a payment UI on top of microservices (payment, order, fraud).”
2. Create a payment

In the UI, fill in:
Order ID, User ID, Amount, Currency.
Submit and show:
New payment row with status and ID.
Say: “The UI calls payment-service (/api/payments), which persists the payment and emits events. We also support idempotency via an Idempotency-Key header to avoid duplicates.”
3. Show idempotency (quick explanation, optional UI + curl)

Explain: “If the client retries with the same Idempotency-Key, the service returns the same payment instead of creating a new one.”
You can mention the curl from HOW_TO_RUN.md if asked:
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-123" \
  -d '{"orderId":"ORD-1","userId":"USER-1","amount":99.99,"currency":"USD"}'
4. Show Fraud Rules tab

Click the Fraud Rules tab in the UI.
If fraud-service is running:
You’ll see rules loaded from fraud-service (/api/fraud/rules).
Say: “Fraud rules live in a separate microservice. The UI calls fraud-service on port 8082 and shows the live configuration (e.g., simple-threshold, maxAmountWithoutReview).”
If fraud-service is not running:
You’ll see the JSON you pasted earlier (fallback rules).
Say: “When fraud isn’t reachable, the UI shows safe defaults with a note telling you to run fraud-service for live data.”
5. Architecture in one sentence

“Behind this UI there are three Spring Boot services communicating over events (Kafka/Azure Event Hubs in full mode), with Terraform + Helm ready to deploy the same setup onto AKS.”

