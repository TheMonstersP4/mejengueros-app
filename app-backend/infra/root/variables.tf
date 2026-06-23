variable "project" {
  description = "Project name used in resource names."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.project))
    error_message = "Use lowercase letters, numbers, and hyphens only."
  }
}

variable "env" {
  description = "Environment name."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.env))
    error_message = "Use lowercase letters, numbers, and hyphens only."
  }
}

variable "aws_region" {
  description = "AWS region for this environment."
  type        = string
  default     = "us-east-2"
}

variable "owner" {
  description = "Owner tag value."
  type        = string
}

variable "cognito_mfa_configuration" {
  description = "Cognito MFA mode: OFF, OPTIONAL, or ON."
  type        = string
  default     = "OFF"

  validation {
    condition     = contains(["OFF", "OPTIONAL", "ON"], var.cognito_mfa_configuration)
    error_message = "MFA configuration must be OFF, OPTIONAL, or ON."
  }
}

variable "cognito_password_minimum_length" {
  description = "Minimum password length for Cognito users."
  type        = number
  default     = 12
}

variable "cognito_self_signup_enabled" {
  description = "Allow users to register themselves with email and password."
  type        = bool
  default     = true
}

variable "cognito_email_verification_subject" {
  description = "Subject for Cognito email verification messages."
  type        = string
  default     = "Verify your Mejengueros account"
}

variable "cognito_email_verification_message" {
  description = "Email verification message. Use {####} where Cognito should place the code."
  type        = string
  default     = "Your Mejengueros verification code is {####}."
}

variable "cognito_callback_urls" {
  description = "Allowed OAuth callback URLs."
  type        = list(string)
}

variable "cognito_logout_urls" {
  description = "Allowed OAuth logout URLs."
  type        = list(string)
}

variable "cognito_admin_users" {
  description = "Optional bootstrap admin users."
  type = list(object({
    email              = string
    temporary_password = optional(string)
  }))
  default   = []
  sensitive = true
}

variable "google_enabled" {
  description = "Enable Google as a Cognito identity provider."
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

variable "microsoft_tenant_id" {
  description = "Microsoft authority tenant value. Use 9188040d-6c67-4c5b-b112-36a304b66dad for personal Outlook, Hotmail, Live, and Skype accounts."
  type        = string
  default     = "9188040d-6c67-4c5b-b112-36a304b66dad"
}

variable "microsoft_provider_name" {
  description = "Provider name shown inside Cognito for Microsoft OIDC."
  type        = string
  default     = "Microsoft"
}

variable "azuread_enabled" {
  description = "Create the Microsoft Entra OAuth application with Terraform and pass it to Cognito."
  type        = bool
  default     = false
}

variable "azuread_sign_in_audience" {
  description = "Supported Microsoft account audience for the generated Entra app registration."
  type        = string
  default     = "PersonalMicrosoftAccount"

  validation {
    condition = contains([
      "AzureADMyOrg",
      "AzureADMultipleOrgs",
      "AzureADandPersonalMicrosoftAccount",
      "PersonalMicrosoftAccount"
    ], var.azuread_sign_in_audience)
    error_message = "Use a supported Microsoft Entra sign-in audience."
  }
}

variable "azuread_homepage_url" {
  description = "Optional homepage URL for the generated Microsoft Entra app registration."
  type        = string
  default     = null
  nullable    = true
}

variable "azuread_logout_url" {
  description = "Optional front-channel logout URL for the generated Microsoft Entra app registration."
  type        = string
  default     = null
  nullable    = true
}

variable "azuread_owner_object_ids" {
  description = "Optional Microsoft Entra object IDs that own the generated app registration."
  type        = list(string)
  default     = []
}

variable "azuread_client_secret_display_name" {
  description = "Display name for the generated Microsoft Entra client secret."
  type        = string
  default     = "cognito"
}

variable "azuread_client_secret_end_date" {
  description = "Optional Microsoft Entra client secret expiration date in RFC3339 format."
  type        = string
  default     = null
  nullable    = true
}

variable "cloudflare_api_token" {
  description = "Cloudflare API token used for optional DNS records."
  type        = string
  default     = ""
  sensitive   = true
}

variable "cloudflare_zone_id" {
  description = "Cloudflare zone ID used for optional DNS records."
  type        = string
  default     = ""
}

variable "cloudflare_account_id" {
  description = "Cloudflare account ID used for optional Workers."
  type        = string
  default     = ""
}

variable "poc_site_enabled" {
  description = "Create an S3 static website bucket for the POC web client."
  type        = bool
  default     = false
}

variable "poc_site_cloudflare_enabled" {
  description = "Create a Cloudflare CNAME for the POC static website."
  type        = bool
  default     = false
}

variable "poc_site_cloudflare_worker_enabled" {
  description = "Create a Cloudflare Worker that proxies the POC site to the S3 website endpoint."
  type        = bool
  default     = false
}

variable "poc_site_domain_name" {
  description = "Root domain for the POC site."
  type        = string
  default     = ""
}

variable "poc_site_subdomain" {
  description = "Subdomain for the POC site."
  type        = string
  default     = ""
}

variable "poc_site_force_destroy" {
  description = "Allow Terraform to delete the POC site bucket even if it contains objects."
  type        = bool
  default     = false
}

variable "github_actions_enabled" {
  description = "Create the GitHub Actions OIDC deploy role."
  type        = bool
  default     = false
}

variable "github_repository" {
  description = "Primary GitHub repository in owner/name format. Kept for compatibility; prefer github_repositories for new projects."
  type        = string
  default     = ""
}

variable "github_repositories" {
  description = "GitHub repositories in owner/name format allowed to deploy through GitHub Actions."
  type        = list(string)
  default     = []
}

variable "github_branch" {
  description = "Git branch allowed to deploy through GitHub Actions."
  type        = string
  default     = "main"
}

variable "github_oidc_provider_arn" {
  description = "Existing GitHub OIDC provider ARN. Leave empty to create one."
  type        = string
  default     = ""
}

variable "postgres_enabled" {
  description = "Create PostgreSQL RDS resources."
  type        = bool
  default     = false
}

variable "postgres_db_name" {
  description = "Initial PostgreSQL database name."
  type        = string
  default     = "appdb"
}

variable "postgres_master_username" {
  description = "PostgreSQL master username."
  type        = string
  default     = "appadmin"
}

variable "postgres_master_password" {
  description = "Optional PostgreSQL master password. If omitted, Terraform generates one."
  type        = string
  default     = null
  nullable    = true
  sensitive   = true
}

variable "postgres_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "postgres_allocated_storage" {
  description = "Initial RDS storage in GB."
  type        = number
  default     = 20
}

variable "postgres_max_allocated_storage" {
  description = "Maximum autoscaled RDS storage in GB. Use 0 to disable autoscaling."
  type        = number
  default     = 100
}

variable "postgres_engine_version" {
  description = "PostgreSQL engine version. Empty string lets AWS choose the default for the engine."
  type        = string
  default     = ""
}

variable "postgres_publicly_accessible" {
  description = "Whether the RDS instance gets a public endpoint."
  type        = bool
  default     = false
}

variable "postgres_allowed_cidr_blocks" {
  description = "CIDR blocks allowed to connect to PostgreSQL."
  type        = list(string)
  default     = []
}

variable "network_vpc_cidr" {
  description = "CIDR block for the project VPC. This module does not create NAT gateways."
  type        = string
  default     = "10.40.0.0/16"
}

variable "postgres_deletion_protection" {
  description = "Protect the RDS instance from deletion."
  type        = bool
  default     = false
}

variable "postgres_skip_final_snapshot" {
  description = "Skip final snapshot when destroying the DB."
  type        = bool
  default     = true
}

variable "postgres_backup_retention_period" {
  description = "RDS backup retention in days."
  type        = number
  default     = 7
}

variable "s3_bucket_name" {
  description = "Optional fixed S3 bucket name. Leave empty to generate a unique name."
  type        = string
  default     = ""
}

variable "s3_versioning" {
  description = "Enable S3 bucket versioning."
  type        = bool
  default     = false
}

variable "s3_force_destroy" {
  description = "Allow Terraform to delete the bucket even if it contains objects."
  type        = bool
  default     = false
}

variable "s3_upload_expiration_days" {
  description = "Days before pending direct-upload objects expire. Use 0 to disable cleanup."
  type        = number
  default     = 30

  validation {
    condition     = var.s3_upload_expiration_days >= 0 && var.s3_upload_expiration_days <= 365
    error_message = "Use a value between 0 and 365 days."
  }
}

variable "ecr_image_tag_mutability" {
  description = "ECR image tag mutability: MUTABLE or IMMUTABLE."
  type        = string
  default     = "MUTABLE"

  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.ecr_image_tag_mutability)
    error_message = "Use MUTABLE or IMMUTABLE."
  }
}

variable "ecr_scan_on_push" {
  description = "Scan ECR images when pushed."
  type        = bool
  default     = true
}

variable "ecr_keep_last_images" {
  description = "Number of recent ECR images to keep."
  type        = number
  default     = 10
}

variable "api_http_enabled" {
  description = "Create the public HTTP API and Lambda container integration."
  type        = bool
  default     = false
}

variable "api_lambda_image_tag" {
  description = "ECR image tag used by the HTTP API Lambda. The repository URL is derived from the project ECR repository."
  type        = string
  default     = "latest"
}

variable "api_lambda_memory_size" {
  description = "HTTP API Lambda memory size in MB."
  type        = number
  default     = 512
}

variable "api_lambda_timeout" {
  description = "HTTP API Lambda timeout in seconds."
  type        = number
  default     = 30
}

variable "api_lambda_vpc_enabled" {
  description = "Attach the HTTP API Lambda to private subnets so it can reach private RDS. This does not create a NAT gateway."
  type        = bool
  default     = false
}

variable "api_database_secret_enabled" {
  description = "Create an AWS Secrets Manager secret for an external API database URL."
  type        = bool
  default     = false
}

variable "api_database_secret_arn" {
  description = "Existing AWS Secrets Manager secret ARN containing the API database URL."
  type        = string
  default     = ""
}

variable "api_log_level" {
  description = "Log level passed to the API runtime."
  type        = string
  default     = "info"
}

variable "api_cors_allowed_origins" {
  description = "Browser origins allowed to call the HTTP API and upload directly to S3."
  type        = list(string)
  default     = []
}

variable "api_cognito_token_use" {
  description = "Expected Cognito token use for API authorization."
  type        = string
  default     = "id"
}

variable "api_s3_upload_url_ttl_seconds" {
  description = "Time-to-live in seconds for API-generated S3 upload URLs."
  type        = number
  default     = 300

  validation {
    condition     = var.api_s3_upload_url_ttl_seconds > 0 && var.api_s3_upload_url_ttl_seconds <= 900
    error_message = "Use a value between 1 and 900 seconds."
  }
}

variable "api_s3_profile_image_max_bytes" {
  description = "Maximum profile image upload size in bytes."
  type        = number
  default     = 5242880

  validation {
    condition     = var.api_s3_profile_image_max_bytes > 0 && var.api_s3_profile_image_max_bytes <= 8388608
    error_message = "Use a value between 1 byte and 8388608 bytes."
  }
}

variable "api_s3_allowed_image_mime_types" {
  description = "Allowed MIME types for API image uploads."
  type        = list(string)
  default     = ["image/jpeg", "image/png", "image/webp"]
}

variable "error_documentation_base_url" {
  description = "Base URL for public error documentation links returned by the API."
  type        = string
  default     = ""
}

variable "http_api_stage_name" {
  description = "HTTP API Gateway stage name. Use $default for the cheapest simple deployment path."
  type        = string
  default     = "$default"
}

variable "http_api_access_log_enabled" {
  description = "Enable CloudWatch access logs for the HTTP API."
  type        = bool
  default     = true
}

variable "http_api_log_retention_days" {
  description = "CloudWatch log retention in days for HTTP API access logs."
  type        = number
  default     = 14
}

variable "websocket_route_selection_expression" {
  description = "JSON expression API Gateway uses to choose WebSocket routes."
  type        = string
  default     = "$request.body.action"
}

variable "websocket_stage_name" {
  description = "WebSocket API stage name."
  type        = string
  default     = "v1"
}

variable "websocket_auto_deploy" {
  description = "Automatically deploy changes to the WebSocket API stage."
  type        = bool
  default     = true
}

variable "websocket_access_log_enabled" {
  description = "Enable CloudWatch access logs for the WebSocket API stage."
  type        = bool
  default     = true
}

variable "websocket_log_retention_days" {
  description = "CloudWatch log retention in days for WebSocket access logs."
  type        = number
  default     = 14
}

variable "websocket_default_route_enabled" {
  description = "Create a default WebSocket route without integration."
  type        = bool
  default     = false
}

variable "websocket_connections_table_name" {
  description = "Optional fixed DynamoDB table name for WebSocket connections."
  type        = string
  default     = ""
}

variable "websocket_connections_ttl_attribute_name" {
  description = "DynamoDB TTL attribute name for stale WebSocket connections."
  type        = string
  default     = "expiresAt"
}

variable "websocket_connections_point_in_time_recovery_enabled" {
  description = "Enable point-in-time recovery for the WebSocket connections table."
  type        = bool
  default     = false
}

variable "websocket_lambda_enabled" {
  description = "Create WebSocket route Lambdas when a deployment zip is provided."
  type        = bool
  default     = false
}

variable "websocket_lambda_package_filename" {
  description = "Path to the WebSocket Lambda deployment zip. Leave empty to skip route Lambdas."
  type        = string
  default     = ""
}

variable "websocket_lambda_package_source_code_hash" {
  description = "Base64-encoded hash of the WebSocket Lambda deployment zip."
  type        = string
  default     = ""
}

variable "websocket_lambda_runtime" {
  description = "Runtime used by WebSocket zip Lambdas."
  type        = string
  default     = "nodejs22.x"
}

variable "websocket_lambda_memory_size" {
  description = "WebSocket Lambda memory size in MB."
  type        = number
  default     = 256
}

variable "websocket_lambda_timeout" {
  description = "WebSocket Lambda timeout in seconds."
  type        = number
  default     = 10
}

variable "websocket_connection_ttl_seconds" {
  description = "Seconds before a WebSocket connection record expires in DynamoDB."
  type        = number
  default     = 86400
}
