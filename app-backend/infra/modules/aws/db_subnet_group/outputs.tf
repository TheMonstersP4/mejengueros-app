output "db_subnet_group_name" {
  description = "DB subnet group name."
  value       = aws_db_subnet_group.subnet_group.name
}

output "db_subnet_group_arn" {
  description = "DB subnet group ARN."
  value       = aws_db_subnet_group.subnet_group.arn
}
