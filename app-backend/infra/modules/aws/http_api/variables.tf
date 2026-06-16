variable "name_prefix" {
  description = "Prefix used for HTTP API resources."
  type        = string
}

variable "stage_name" {
  description = "HTTP API stage name."
  type        = string
  default     = "$default"
}

variable "auto_deploy" {
  description = "Automatically deploy HTTP API changes."
  type        = bool
  default     = true
}

variable "lambda_invoke_arn" {
  description = "Lambda invoke ARN used by the HTTP API integration."
  type        = string
}

variable "lambda_function_name" {
  description = "Lambda function name allowed to receive API Gateway invokes."
  type        = string
}

variable "route_key" {
  description = "HTTP API route key."
  type        = string
  default     = "$default"
}

variable "access_log_enabled" {
  description = "Enable CloudWatch access logs for the HTTP API stage."
  type        = bool
  default     = true
}

variable "access_log_retention_days" {
  description = "CloudWatch log retention in days for HTTP API access logs."
  type        = number
  default     = 14
}

variable "cors_allowed_origins" {
  description = "Browser origins allowed to call the HTTP API. Empty disables API Gateway CORS."
  type        = list(string)
  default     = []
}

variable "cors_allowed_methods" {
  description = "HTTP methods allowed by API Gateway CORS."
  type        = list(string)
  default     = ["GET", "POST", "OPTIONS"]
}

variable "cors_allowed_headers" {
  description = "HTTP request headers allowed by API Gateway CORS."
  type        = list(string)
  default     = ["Authorization", "Content-Type"]
}

variable "cors_max_age_seconds" {
  description = "Browser CORS preflight cache time in seconds."
  type        = number
  default     = 300
}
