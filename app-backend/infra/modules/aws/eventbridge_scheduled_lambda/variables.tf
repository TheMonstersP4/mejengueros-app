variable "name" {
  description = "EventBridge rule name."
  type        = string
}

variable "description" {
  description = "EventBridge rule description."
  type        = string
  default     = ""
}

variable "schedule_expression" {
  description = "EventBridge schedule expression."
  type        = string
}

variable "target_id" {
  description = "EventBridge target ID."
  type        = string
  default     = "lambda"
}

variable "lambda_function_name" {
  description = "Lambda function name allowed to be invoked by EventBridge."
  type        = string
}

variable "lambda_function_arn" {
  description = "Lambda function ARN used as the EventBridge target."
  type        = string
}

variable "permission_statement_id" {
  description = "Lambda permission statement ID for EventBridge invocation."
  type        = string
  default     = "AllowExecutionFromEventBridge"
}

variable "failed_invocations_alarm_actions" {
  description = "CloudWatch alarm action ARNs for EventBridge failed invocations."
  type        = list(string)
  default     = []
}

variable "failed_invocations_alarm_name" {
  description = "CloudWatch alarm name for EventBridge failed invocations."
  type        = string
  default     = ""
}
