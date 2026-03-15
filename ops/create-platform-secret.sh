#!/bin/bash
# Create the platform-config Kubernetes secret for microservices.
# Run after Terraform apply - use outputs for the values.
#
# Usage:
#   ./create-platform-secret.sh <postgresql-fqdn> <eventhubs-namespace> <eventhubs-connection-string> [db-username] [db-password]
#
# Example (replace with your Terraform outputs):
#   terraform -chdir=infra/terraform/envs/dev output -json > /tmp/tf-outputs.json
#   PG_FQDN=$(jq -r '.postgresql_fqdn.value' /tmp/tf-outputs.json)
#   EH_NS=$(jq -r '.event_hubs_namespace.value' /tmp/tf-outputs.json)
#   EH_CS=$(jq -r '.event_hubs_connection_string.value' /tmp/tf-outputs.json)
#   ./create-platform-secret.sh "$PG_FQDN" "$EH_NS" "$EH_CS" cnppadmin "your-db-password"

set -e

PG_FQDN=${1:? "Usage: $0 <postgresql-fqdn> <eventhubs-namespace> <eventhubs-connection-string> [db-username] [db-password]"}
EH_NS=${2:?}
EH_CS=${3:?}
DB_USER=${4:-cnppadmin}
DB_PASS=${5:? "DB password required"}

DB_URL="jdbc:postgresql://${PG_FQDN}:5432/paymentdb?sslmode=require"
KAFKA_BOOTSTRAP="${EH_NS}.servicebus.windows.net:9093"
# Event Hubs Kafka: SASL JAAS config with connection string as password
KAFKA_SASL_JAAS="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"\$ConnectionString\" password=\"${EH_CS}\";"

kubectl create secret generic platform-config \
  --namespace=dev \
  --from-literal=DB_URL="$DB_URL" \
  --from-literal=DB_USERNAME="$DB_USER" \
  --from-literal=DB_PASSWORD="$DB_PASS" \
  --from-literal=KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP" \
  --from-literal=EVENTHUB_CONNECTION_STRING="$EH_CS" \
  --from-literal=KAFKA_SASL_JAAS_CONFIG="$KAFKA_SASL_JAAS" \
  --from-literal=KAFKA_SECURITY_PROTOCOL="SASL_SSL" \
  --from-literal=KAFKA_SASL_MECHANISM="PLAIN" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Secret platform-config created/updated in namespace dev."
