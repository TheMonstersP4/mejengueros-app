variable "repository_name" {
  description = "ECR repository name."
  type        = string
}

variable "image_tag_mutability" {
  description = "Image tag mutability: MUTABLE or IMMUTABLE."
  type        = string
  default     = "MUTABLE"
}

variable "scan_on_push" {
  description = "Scan images when pushed."
  type        = bool
  default     = true
}

variable "keep_last_images" {
  description = "Number of recent images to keep."
  type        = number
  default     = 10
}
