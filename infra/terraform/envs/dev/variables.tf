variable "resource_group_name" {
  description = "Name of the Azure Resource Group for dev environment"
  type        = string
}

variable "location" {
  description = "Azure region for all resources"
  type        = string
  default     = "eastus"
}

variable "vnet_cidr" {
  description = "CIDR for the VNet"
  type        = string
  default     = "10.20.0.0/16"
}

variable "aks_subnet_cidr" {
  description = "CIDR for the AKS subnet"
  type        = string
  default     = "10.20.1.0/24"
}

variable "aks_cluster_name" {
  description = "Name of the AKS cluster"
  type        = string
  default     = "cnpp-aks-dev"
}

variable "aks_dns_prefix" {
  description = "DNS prefix for AKS"
  type        = string
  default     = "cnpp-dev"
}

variable "aks_node_count" {
  description = "Number of nodes in the AKS node pool"
  type        = number
  default     = 1
}

variable "db_name" {
  description = "Name of the PostgreSQL database"
  type        = string
  default     = "paymentdb"
}

variable "db_admin_username" {
  description = "PostgreSQL admin username"
  type        = string
}

variable "db_admin_password" {
  description = "PostgreSQL admin password"
  type        = string
  sensitive   = true
}

variable "event_hubs_namespace" {
  description = "Azure Event Hubs namespace name"
  type        = string
  default     = "cnpp-dev-eh"
}

variable "key_vault_name" {
  description = "Azure Key Vault name"
  type        = string
}

variable "log_analytics_workspace_name" {
  description = "Log Analytics workspace name"
  type        = string
  default     = "cnpp-log-dev"
}

variable "acr_name" {
  description = "Azure Container Registry name (alphanumeric only, 5-50 chars, globally unique)"
  type        = string
  default     = "cnppacrdev"
}

