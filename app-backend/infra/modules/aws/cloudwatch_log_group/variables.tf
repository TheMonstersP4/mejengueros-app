variable "name" {
  description = "CloudWatch log group name."
  type        = string
}

variable "retention_in_days" {
  description = "CloudWatch log retention in days."
  type        = number
  default     = 14
}

variable "kms_key_id" {
  description = "Optional KMS key ID or ARN for log group encryption."
  type        = string
  default     = null
  nullable    = true
}
