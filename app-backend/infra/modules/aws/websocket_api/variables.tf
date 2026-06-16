variable "name_prefix" {
  description = "Prefix used for WebSocket API resources."
  type        = string
}

variable "route_selection_expression" {
  description = "JSON expression API Gateway uses to choose WebSocket routes."
  type        = string
  default     = "$request.body.action"
}

variable "stage_name" {
  description = "WebSocket API stage name."
  type        = string
  default     = "v1"
}

variable "auto_deploy" {
  description = "Automatically deploy changes to the WebSocket API stage."
  type        = bool
  default     = true
}

variable "access_log_enabled" {
  description = "Enable CloudWatch access logs for the WebSocket stage."
  type        = bool
  default     = true
}

variable "access_log_retention_days" {
  description = "CloudWatch log retention in days for WebSocket access logs."
  type        = number
  default     = 14
}

variable "default_route_enabled" {
  description = "Create a default route without integration. Set false until an integration exists."
  type        = bool
  default     = false
}

variable "lambda_routes" {
  description = "WebSocket routes integrated with Lambda functions. Keys must be route keys such as $connect, $disconnect, or $default."
  type = map(object({
    invoke_arn    = string
    function_name = string
  }))
  default = {}
}

variable "connections_table_name" {
  description = "Optional fixed DynamoDB table name for WebSocket connections."
  type        = string
  default     = ""
}

variable "connections_ttl_attribute_name" {
  description = "DynamoDB TTL attribute name for stale WebSocket connections."
  type        = string
  default     = "expiresAt"
}

variable "connections_point_in_time_recovery_enabled" {
  description = "Enable point-in-time recovery for the WebSocket connections table."
  type        = bool
  default     = false
}
