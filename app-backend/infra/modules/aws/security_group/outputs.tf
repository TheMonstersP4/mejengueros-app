output "security_group_id" {
  description = "Security group ID."
  value       = aws_security_group.security_group.id
}

output "security_group_arn" {
  description = "Security group ARN."
  value       = aws_security_group.security_group.arn
}

output "security_group_name" {
  description = "Security group name."
  value       = aws_security_group.security_group.name
}
