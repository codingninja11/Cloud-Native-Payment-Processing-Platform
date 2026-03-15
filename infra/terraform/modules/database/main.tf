resource "azurerm_postgresql_flexible_server" "this" {
  name                   = "${var.db_name}-server"
  resource_group_name    = var.resource_group_name
  location               = var.location
  administrator_login    = var.admin_username
  administrator_password = var.admin_password

  sku_name = "B_Standard_B1ms"

  storage_mb = 32768

  high_availability {
    mode = "Disabled"
  }

  backup {
    backup_retention_days = 7
  }
}

resource "azurerm_postgresql_flexible_server_database" "db" {
  name      = var.db_name
  server_id = azurerm_postgresql_flexible_server.this.id
}

output "fqdn" {
  value = azurerm_postgresql_flexible_server.this.fqdn
}

