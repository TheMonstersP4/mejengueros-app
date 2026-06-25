variable "name_prefix" {
  description = "Prefix used for Cognito resource names."
  type        = string
}

variable "domain_prefix" {
  description = "Cognito hosted UI domain prefix."
  type        = string
}

variable "mfa_configuration" {
  description = "MFA mode: OFF, OPTIONAL, or ON."
  type        = string
  default     = "OFF"
}

variable "password_minimum_length" {
  description = "Minimum password length."
  type        = number
  default     = 12
}

variable "self_signup_enabled" {
  description = "Allow users to register themselves with email and password."
  type        = bool
  default     = true
}

variable "email_verification_subject" {
  description = "Subject for Cognito email verification messages."
  type        = string
  default     = "Verify your Mejengueros account"
}

variable "email_verification_message" {
  description = "Email verification message. Use {####} where Cognito should place the code."
  type        = string
  default     = "Your Mejengueros verification code is {####}."
}

variable "callback_urls" {
  description = "Allowed OAuth callback URLs."
  type        = list(string)
}

variable "logout_urls" {
  description = "Allowed OAuth logout URLs."
  type        = list(string)
}

variable "admin_users" {
  description = "Optional bootstrap admin users."
  type = list(object({
    email              = string
    temporary_password = optional(string)
  }))
  default   = []
  sensitive = true
}

variable "google_enabled" {
  description = "Enable Google login."
  type        = bool
  default     = false
}

variable "google_client_id" {
  description = "Google OAuth client ID."
  type        = string
  default     = ""
}

variable "google_client_secret" {
  description = "Google OAuth client secret."
  type        = string
  default     = ""
  sensitive   = true
}

variable "microsoft_enabled" {
  description = "Enable Microsoft login through OIDC."
  type        = bool
  default     = false
}

variable "microsoft_tenant_id" {
  description = "Microsoft authority tenant value. Use 9188040d-6c67-4c5b-b112-36a304b66dad for personal Outlook, Hotmail, Live, and Skype accounts."
  type        = string
  default     = "9188040d-6c67-4c5b-b112-36a304b66dad"
}

variable "microsoft_client_id" {
  description = "Microsoft OAuth client ID."
  type        = string
  default     = ""
}

variable "microsoft_client_secret" {
  description = "Microsoft OAuth client secret."
  type        = string
  default     = ""
  sensitive   = true
}

variable "microsoft_provider_name" {
  description = "Cognito provider name for Microsoft."
  type        = string
  default     = "Microsoft"
}
