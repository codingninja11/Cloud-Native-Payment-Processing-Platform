resource "azurerm_kubernetes_cluster" "this" {
  name                = var.cluster_name
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix          = var.dns_prefix

  default_node_pool {
    name                = "system"
    node_count          = var.node_count
    vm_size             = "Standard_B4ms"
    vnet_subnet_id      = var.vnet_subnet_id
    enable_auto_scaling = false
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin    = "azure"
    load_balancer_sku = "standard"
  }
}

output "cluster_name" {
  value = azurerm_kubernetes_cluster.this.name
}

output "kube_config" {
  value = azurerm_kubernetes_cluster.this.kube_config_raw
}

output "identity_principal_id" {
  description = "Principal ID of the AKS cluster's system-assigned identity"
  value       = azurerm_kubernetes_cluster.this.identity[0].principal_id
}

