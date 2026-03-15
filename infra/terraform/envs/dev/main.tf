terraform {
  backend "azurerm" {
    # Configure these before running terraform init
    resource_group_name  = "REPLACE_ME_TFSTATE_RG"
    storage_account_name = "replacemestorageacct"
    container_name       = "tfstate"
    key                  = "dev/terraform.tfstate"
  }
}

provider "azurerm" {
  features {}
}

module "networking" {
  source              = "../../modules/networking"
  resource_group_name = var.resource_group_name
  location            = var.location
  vnet_cidr           = var.vnet_cidr
  aks_subnet_cidr     = var.aks_subnet_cidr
}

module "aks" {
  source              = "../../modules/aks"
  resource_group_name = var.resource_group_name
  location            = var.location
  cluster_name        = var.aks_cluster_name
  dns_prefix          = var.aks_dns_prefix
  vnet_subnet_id      = module.networking.aks_subnet_id
  node_count          = var.aks_node_count
}

module "database" {
  source              = "../../modules/database"
  resource_group_name = var.resource_group_name
  location            = var.location
  db_name             = var.db_name
  admin_username      = var.db_admin_username
  admin_password      = var.db_admin_password
}

module "event_hubs" {
  source              = "../../modules/event_hubs"
  resource_group_name = var.resource_group_name
  location            = var.location
  namespace_name      = var.event_hubs_namespace
}

module "key_vault" {
  source              = "../../modules/key_vault"
  resource_group_name = var.resource_group_name
  location            = var.location
  vault_name          = var.key_vault_name
}

module "log_analytics" {
  source              = "../../modules/log_analytics"
  resource_group_name = var.resource_group_name
  location            = var.location
  workspace_name      = var.log_analytics_workspace_name
}

module "acr" {
  source              = "../../modules/acr"
  resource_group_name = var.resource_group_name
  location            = var.location
  acr_name            = var.acr_name
}

# Grant AKS pull access to ACR
resource "azurerm_role_assignment" "aks_acr_pull" {
  scope                = module.acr.id
  role_definition_name = "AcrPull"
  principal_id         = module.aks.identity_principal_id
}

output "aks_cluster_name" {
  value = module.aks.cluster_name
}

output "aks_kube_config" {
  value     = module.aks.kube_config
  sensitive = true
}

output "postgresql_fqdn" {
  value = module.database.fqdn
}

output "event_hubs_namespace" {
  value = module.event_hubs.namespace_name
}

output "event_hubs_connection_string" {
  value     = module.event_hubs.connection_string
  sensitive = true
}

output "key_vault_uri" {
  value = module.key_vault.vault_uri
}

output "acr_name" {
  value = module.acr.name
}

output "acr_login_server" {
  value = module.acr.login_server
}

