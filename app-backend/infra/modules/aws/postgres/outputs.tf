output "db_instance_id" {
  description = "RDS instance identifier."
  value       = aws_db_instance.database.id
}

output "endpoint" {
  description = "RDS endpoint."
  value       = aws_db_instance.database.address
}

output "port" {
  description = "RDS port."
  value       = aws_db_instance.database.port
}

output "db_name" {
  description = "Database name."
  value       = aws_db_instance.database.db_name
}

output "security_group_id" {
  description = "First security group attached to PostgreSQL."
  value       = var.security_group_ids[0]
}

output "vpc_id" {
  description = "Deprecated. VPC is managed outside this module."
  value       = null
}

output "subnet_ids" {
  description = "Deprecated. Subnets are managed outside this module."
  value       = null
}

output "master_password" {
  description = "Master password. Sensitive by design."
  value       = local.master_password
  sensitive   = true
}
