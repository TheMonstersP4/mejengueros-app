variable "bucket_name" {
  description = "S3 bucket name. For S3 website custom domains, use the full domain name."
  type        = string
}

variable "force_destroy" {
  description = "Allow Terraform to delete the bucket even if it contains objects."
  type        = bool
  default     = false
}
