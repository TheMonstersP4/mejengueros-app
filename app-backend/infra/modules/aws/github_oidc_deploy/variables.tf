variable "name_prefix" {
  description = "Prefix used in IAM resource names."
  type        = string
}

variable "repository" {
  description = "Primary GitHub repository in owner/name format. Kept for compatibility; prefer repositories for new projects."
  type        = string
  default     = ""
}

variable "repositories" {
  description = "GitHub repositories in owner/name format allowed to assume the deploy role."
  type        = list(string)
  default     = []
}

variable "branch" {
  description = "Git branch allowed to assume the role."
  type        = string
  default     = "main"
}

variable "environment" {
  description = "Optional GitHub environment name allowed to assume the role."
  type        = string
  default     = ""
}

variable "existing_oidc_provider_arn" {
  description = "Existing GitHub OIDC provider ARN. Leave empty to create one."
  type        = string
  default     = ""
}

variable "poc_site_bucket_name" {
  description = "S3 bucket name allowed for POC site deploy."
  type        = string
  default     = ""
}

variable "ecr_repository_arn" {
  description = "ECR repository ARN allowed for Docker pushes."
  type        = string
  default     = ""
}

variable "lambda_function_arns" {
  description = "Lambda function ARNs allowed for code updates."
  type        = list(string)
  default     = []
}

variable "secrets_manager_secret_arns" {
  description = "Secrets Manager secret ARNs allowed for deploy-time secret updates."
  type        = list(string)
  default     = []
}
