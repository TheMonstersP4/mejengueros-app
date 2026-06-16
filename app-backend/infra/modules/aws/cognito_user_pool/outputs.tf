output "user_pool_id" {
  description = "Cognito user pool ID."
  value       = aws_cognito_user_pool.user_pool.id
}

output "user_pool_arn" {
  description = "Cognito user pool ARN."
  value       = aws_cognito_user_pool.user_pool.arn
}

output "user_pool_client_id" {
  description = "Cognito app client ID."
  value       = aws_cognito_user_pool_client.app_client.id
}

output "domain" {
  description = "Cognito domain prefix."
  value       = aws_cognito_user_pool_domain.domain.domain
}

output "domain_url" {
  description = "Cognito hosted UI base URL."
  value       = "https://${aws_cognito_user_pool_domain.domain.domain}.auth.${data.aws_region.current.name}.amazoncognito.com"
}

output "identity_provider_redirect_url" {
  description = "Redirect URL required by external identity providers such as Google and Microsoft Entra."
  value       = "https://${aws_cognito_user_pool_domain.domain.domain}.auth.${data.aws_region.current.name}.amazoncognito.com/oauth2/idpresponse"
}

output "admin_group_name" {
  description = "Admin group name."
  value       = aws_cognito_user_group.admin.name
}
