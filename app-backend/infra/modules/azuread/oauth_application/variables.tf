variable "display_name" {
  description = "Microsoft Entra application display name."
  type        = string
}

variable "sign_in_audience" {
  description = "Supported account audience for the app registration."
  type        = string
  default     = "PersonalMicrosoftAccount"

  validation {
    condition = contains([
      "AzureADMyOrg",
      "AzureADMultipleOrgs",
      "AzureADandPersonalMicrosoftAccount",
      "PersonalMicrosoftAccount"
    ], var.sign_in_audience)
    error_message = "Use a supported Microsoft Entra sign-in audience."
  }
}

variable "redirect_uris" {
  description = "OAuth redirect URIs registered in Microsoft Entra."
  type        = list(string)
}

variable "homepage_url" {
  description = "Optional application homepage URL."
  type        = string
  default     = null
  nullable    = true
}

variable "logout_url" {
  description = "Optional front-channel logout URL."
  type        = string
  default     = null
  nullable    = true
}

variable "owner_object_ids" {
  description = "Optional Microsoft Entra object IDs that own the application."
  type        = list(string)
  default     = []
}

variable "client_secret_display_name" {
  description = "Display name for the generated client secret."
  type        = string
  default     = "cognito"
}

variable "client_secret_end_date" {
  description = "Client secret expiration date in RFC3339 format."
  type        = string
  default     = null
  nullable    = true
}
