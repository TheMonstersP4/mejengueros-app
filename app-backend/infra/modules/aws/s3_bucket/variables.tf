variable "name_prefix" {
  description = "Prefix used when generating the bucket name."
  type        = string
}

variable "bucket_name" {
  description = "Optional fixed bucket name."
  type        = string
  default     = ""
}

variable "purpose" {
  description = "Short purpose suffix used when bucket_name is empty."
  type        = string
  default     = "app"
}

variable "versioning" {
  description = "Enable bucket versioning."
  type        = bool
  default     = false
}

variable "force_destroy" {
  description = "Allow Terraform to delete non-empty buckets."
  type        = bool
  default     = false
}

variable "cors_allowed_origins" {
  description = "Browser origins allowed to upload directly to this bucket. Empty disables CORS."
  type        = list(string)
  default     = []
}

variable "cors_allowed_methods" {
  description = "S3 methods allowed by bucket CORS."
  type        = list(string)
  default     = ["POST"]
}

variable "cors_allowed_headers" {
  description = "Request headers allowed by bucket CORS."
  type        = list(string)
  default     = ["*"]
}

variable "cors_expose_headers" {
  description = "Response headers exposed to browser clients."
  type        = list(string)
  default     = ["ETag"]
}

variable "cors_max_age_seconds" {
  description = "Browser CORS preflight cache time in seconds."
  type        = number
  default     = 300
}

variable "lifecycle_expiration_rules" {
  description = "Prefix-based object expiration rules."
  type = list(object({
    id     = string
    prefix = string
    days   = number
  }))
  default = []
}
