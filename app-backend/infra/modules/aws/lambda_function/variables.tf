variable "function_name" {
  description = "Lambda function name."
  type        = string
}

variable "role_arn" {
  description = "IAM role ARN used by the Lambda function."
  type        = string
}

variable "handler" {
  description = "Lambda handler for zip package deployments."
  type        = string
  default     = null
  nullable    = true
}

variable "runtime" {
  description = "Lambda runtime for zip package deployments."
  type        = string
  default     = null
  nullable    = true
}

variable "filename" {
  description = "Path to the Lambda deployment package for zip deployments."
  type        = string
  default     = null
  nullable    = true
}

variable "source_code_hash" {
  description = "Base64-encoded hash of the Lambda deployment package."
  type        = string
  default     = null
  nullable    = true
}

variable "image_uri" {
  description = "ECR image URI for image-based Lambda deployments."
  type        = string
  default     = null
  nullable    = true
}

variable "package_type" {
  description = "Lambda package type: Zip or Image."
  type        = string
  default     = "Zip"

  validation {
    condition     = contains(["Zip", "Image"], var.package_type)
    error_message = "Package type must be Zip or Image."
  }
}

variable "timeout" {
  description = "Lambda timeout in seconds."
  type        = number
  default     = 30
}

variable "memory_size" {
  description = "Lambda memory size in MB."
  type        = number
  default     = 256
}

variable "environment_variables" {
  description = "Lambda environment variables."
  type        = map(string)
  default     = {}
}

variable "subnet_ids" {
  description = "Optional VPC subnet IDs for Lambda networking."
  type        = list(string)
  default     = []
}

variable "security_group_ids" {
  description = "Optional security group IDs for Lambda networking."
  type        = list(string)
  default     = []
}

variable "log_group_name" {
  description = "Optional CloudWatch log group name created outside this module."
  type        = string
  default     = null
  nullable    = true
}
