variable "resource_group_name" {
  description = "Name of the Azure Resource Group"
  type        = string
}

variable "location" {
  description = "Azure region"
  type        = string
}

variable "acr_name" {
  description = "Name of the Azure Container Registry (alphanumeric only, 5-50 chars)"
  type        = string
}

variable "sku" {
  description = "ACR SKU: Basic, Standard, or Premium"
  type        = string
  default     = "Basic"
}

variable "admin_enabled" {
  description = "Enable admin user for ACR (useful for CI/CD)"
  type        = bool
  default     = true
}
