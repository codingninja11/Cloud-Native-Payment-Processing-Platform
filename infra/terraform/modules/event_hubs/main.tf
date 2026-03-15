resource "azurerm_eventhub_namespace" "this" {
  name                = var.namespace_name
  location            = var.location
  resource_group_name = var.resource_group_name
  sku                 = "Standard"
  capacity            = 1
  kafka_enabled       = true
}

resource "azurerm_eventhub" "payments" {
  name                = "payments"
  namespace_name      = azurerm_eventhub_namespace.this.name
  resource_group_name = var.resource_group_name
  partition_count     = 4
  message_retention   = 1
}

resource "azurerm_eventhub" "fraud_results" {
  name                = "fraud-results"
  namespace_name      = azurerm_eventhub_namespace.this.name
  resource_group_name = var.resource_group_name
  partition_count     = 2
  message_retention   = 1
}

resource "azurerm_eventhub" "orders" {
  name                = "orders"
  namespace_name      = azurerm_eventhub_namespace.this.name
  resource_group_name = var.resource_group_name
  partition_count     = 2
  message_retention   = 1
}

resource "azurerm_eventhub" "dead_letter" {
  name                = "dead-letter"
  namespace_name      = azurerm_eventhub_namespace.this.name
  resource_group_name = var.resource_group_name
  partition_count     = 2
  message_retention   = 7
}

resource "azurerm_eventhub_namespace_authorization_rule" "kafka" {
  name                = "kafka-access"
  namespace_name      = azurerm_eventhub_namespace.this.name
  resource_group_name = var.resource_group_name
  listen              = true
  send                = true
  manage              = false
}

output "namespace_name" {
  value = azurerm_eventhub_namespace.this.name
}

output "kafka_endpoint" {
  value = azurerm_eventhub_namespace.this.kafka_endpoint
}

output "connection_string" {
  description = "Event Hubs namespace connection string for Kafka clients"
  value       = azurerm_eventhub_namespace_authorization_rule.kafka.primary_connection_string
  sensitive   = true
}

