output "cloudwatch_role_arn" {
  description = "IAM role ARN configured as the regional API Gateway CloudWatch role."
  value       = aws_iam_role.cloudwatch.arn
}

output "cloudwatch_role_name" {
  description = "IAM role name configured as the regional API Gateway CloudWatch role."
  value       = aws_iam_role.cloudwatch.name
}
