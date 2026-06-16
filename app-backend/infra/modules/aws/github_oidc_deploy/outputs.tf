output "role_arn" {
  description = "IAM role ARN for GitHub Actions OIDC."
  value       = aws_iam_role.github_actions.arn
}

output "role_name" {
  description = "IAM role name for GitHub Actions OIDC."
  value       = aws_iam_role.github_actions.name
}
