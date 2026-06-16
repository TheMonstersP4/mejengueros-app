variable "name_prefix" {
  description = "Prefix used for PostgreSQL resources."
  type        = string
}

variable "db_name" {
  description = "Initial database name."
  type        = string
}

variable "master_username" {
  description = "Master username."
  type        = string
}

variable "master_password" {
  description = "Optional master password. If null, a password is generated."
  type        = string
  default     = null
  nullable    = true
  sensitive   = true
}

variable "instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Initial storage in GB."
  type        = number
  default     = 20
}

variable "max_allocated_storage" {
  description = "Maximum autoscaled storage in GB. Use 0 to disable."
  type        = number
  default     = 100
}

variable "engine_version" {
  description = "PostgreSQL engine version. Empty string lets AWS choose."
  type        = string
  default     = ""
}

variable "port" {
  description = "PostgreSQL port."
  type        = number
  default     = 5432
}

variable "publicly_accessible" {
  description = "Whether the DB has a public endpoint."
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "Protect DB from deletion."
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  description = "Skip final snapshot on destroy."
  type        = bool
  default     = true
}

variable "backup_retention_period" {
  description = "Backup retention in days."
  type        = number
  default     = 7
}

variable "db_subnet_group_name" {
  description = "DB subnet group name for the PostgreSQL instance."
  type        = string
}

variable "security_group_ids" {
  description = "Security group IDs attached to the PostgreSQL instance."
  type        = list(string)
}
