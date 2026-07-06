output "cognito_user_pool_id" {
  description = "Cognito user pool ID."
  value       = module.cognito.user_pool_id
}

output "cognito_user_pool_client_id" {
  description = "Cognito app client ID."
  value       = module.cognito.user_pool_client_id
}

output "cognito_domain_url" {
  description = "Cognito hosted UI base URL."
  value       = module.cognito.domain_url
}

output "cognito_identity_provider_redirect_url" {
  description = "Redirect URL to register in Google Cloud and Microsoft Entra OAuth apps."
  value       = module.cognito.identity_provider_redirect_url
}

output "azuread_application_client_id" {
  description = "Microsoft Entra application client ID created by Terraform."
  value       = var.azuread_enabled ? module.microsoft_oauth[0].client_id : null
}

output "azuread_application_object_id" {
  description = "Microsoft Entra application object ID created by Terraform."
  value       = var.azuread_enabled ? module.microsoft_oauth[0].object_id : null
}

output "postgres_endpoint" {
  description = "PostgreSQL endpoint when PostgreSQL is enabled."
  value       = var.postgres_enabled ? module.postgres[0].endpoint : null
}

output "network_vpc_id" {
  description = "Project VPC ID."
  value       = module.network.vpc_id
}

output "network_private_subnet_ids" {
  description = "Private subnet IDs for internal services."
  value       = module.network.private_subnet_ids
}

output "postgres_port" {
  description = "PostgreSQL port when PostgreSQL is enabled."
  value       = var.postgres_enabled ? module.postgres[0].port : null
}

output "postgres_database_name" {
  description = "PostgreSQL database name when PostgreSQL is enabled."
  value       = var.postgres_enabled ? module.postgres[0].db_name : null
}

output "postgres_master_password" {
  description = "Generated PostgreSQL master password when PostgreSQL is enabled and no password was supplied."
  value       = var.postgres_enabled ? module.postgres[0].master_password : null
  sensitive   = true
}

output "s3_bucket_name" {
  description = "Application S3 bucket name."
  value       = module.app_bucket.bucket_name
}

output "s3_bucket_arn" {
  description = "Application S3 bucket ARN."
  value       = module.app_bucket.bucket_arn
}

output "ecr_repository_url" {
  description = "ECR repository URL for Docker pushes."
  value       = module.ecr_api.repository_url
}

output "ecr_repository_name" {
  description = "ECR repository name."
  value       = module.ecr_api.repository_name
}

output "ecr_repository_arn" {
  description = "ECR repository ARN."
  value       = module.ecr_api.repository_arn
}

output "poc_site_bucket_name" {
  description = "POC static site S3 bucket name."
  value       = var.poc_site_enabled ? module.poc_site[0].bucket_name : null
}

output "poc_site_url" {
  description = "POC static site URL."
  value       = var.poc_site_enabled ? "https://${local.poc_site_domain}" : null
}

output "github_actions_iam_role_arn" {
  description = "IAM role ARN for GitHub Actions OIDC deploys."
  value       = var.github_actions_enabled && length(local.github_deploy_repositories) > 0 ? module.github_oidc_deploy[0].role_arn : null
}

output "github_actions_iam_role_name" {
  description = "IAM role name for GitHub Actions OIDC deploys."
  value       = var.github_actions_enabled && length(local.github_deploy_repositories) > 0 ? module.github_oidc_deploy[0].role_name : null
}

output "http_api_id" {
  description = "HTTP API Gateway API ID."
  value       = local.api_http_deploy_enabled ? module.http_api[0].api_id : null
}

output "http_api_endpoint" {
  description = "HTTP API Gateway endpoint."
  value       = local.api_http_deploy_enabled ? module.http_api[0].api_endpoint : null
}

output "http_api_access_log_group_name" {
  description = "CloudWatch access log group name for the HTTP API."
  value       = local.api_http_deploy_enabled ? module.http_api[0].access_log_group_name : null
}

output "api_lambda_function_name" {
  description = "Expected HTTP API Lambda function name."
  value       = "${local.name_prefix}-http"
}

output "reservation_completion_worker_function_name" {
  description = "Expected reservation completion worker Lambda function name."
  value       = "${local.name_prefix}-reservation-completion"
}

output "reservation_completion_worker_schedule_rule_name" {
  description = "EventBridge rule name for the reservation completion worker schedule."
  value       = local.reservation_completion_worker_deploy_enabled ? aws_cloudwatch_event_rule.reservation_completion_worker[0].name : null
}

output "reservation_completion_worker_lambda_error_alarm_name" {
  description = "CloudWatch alarm name for reservation completion worker Lambda invocation errors."
  value       = local.reservation_completion_worker_deploy_enabled ? aws_cloudwatch_metric_alarm.reservation_completion_worker_lambda_errors[0].alarm_name : null
}

output "reservation_completion_worker_failed_invocations_alarm_name" {
  description = "CloudWatch alarm name for reservation completion worker EventBridge failed invocations."
  value       = local.reservation_completion_worker_deploy_enabled ? aws_cloudwatch_metric_alarm.reservation_completion_worker_failed_invocations[0].alarm_name : null
}

output "websocket_api_id" {
  description = "API Gateway WebSocket API ID."
  value       = module.websocket_api.api_id
}

output "websocket_url" {
  description = "Full WebSocket URL including stage."
  value       = module.websocket_api.websocket_url
}

output "websocket_access_log_group_name" {
  description = "CloudWatch access log group name for the WebSocket API."
  value       = module.websocket_api.access_log_group_name
}

output "websocket_manage_connections_arn" {
  description = "IAM resource ARN for posting to connected WebSocket clients."
  value       = module.websocket_api.manage_connections_arn
}

output "websocket_connections_table_name" {
  description = "DynamoDB table name for active WebSocket connections."
  value       = module.websocket_api.connections_table_name
}

output "websocket_connections_table_arn" {
  description = "DynamoDB table ARN for active WebSocket connections."
  value       = module.websocket_api.connections_table_arn
}

output "websocket_connections_room_id_index_name" {
  description = "DynamoDB index name for querying WebSocket connections by room ID."
  value       = module.websocket_api.connections_room_id_index_name
}

output "websocket_connections_user_id_index_name" {
  description = "DynamoDB index name for querying WebSocket connections by user ID."
  value       = module.websocket_api.connections_user_id_index_name
}

output "websocket_lambda_route_keys" {
  description = "WebSocket route keys integrated with Lambda functions."
  value       = module.websocket_api.lambda_route_keys
}

output "deploy_config" {
  description = "Deployment values for GitHub Actions."
  sensitive   = true
  value = {
    API_LAMBDA_BOOTSTRAP_IMAGE_URI     = local.api_lambda_image_uri
    API_LAMBDA_FUNCTION_NAME           = "${local.name_prefix}-http"
    AWS_REGION                         = var.aws_region
    COGNITO_CLIENT_ID                  = module.cognito.user_pool_client_id
    COGNITO_DOMAIN_URL                 = module.cognito.domain_url
    DATABASE_SECRET_ARN                = local.api_database_secret_arn
    ECR_REPOSITORY_NAME                = module.ecr_api.repository_name
    ECR_REPOSITORY_URI                 = module.ecr_api.repository_url
    HTTP_API_ENDPOINT                  = local.api_http_deploy_enabled ? module.http_api[0].api_endpoint : ""
    POC_SITE_BUCKET                    = var.poc_site_enabled ? module.poc_site[0].bucket_name : ""
    POC_SITE_URL                       = var.poc_site_enabled ? "https://${local.poc_site_domain}" : ""
    RESERVATION_COMPLETION_WORKER_FUNCTION_NAME = "${local.name_prefix}-reservation-completion"
    WEBSOCKET_CONNECT_FUNCTION_NAME    = local.websocket_lambda_deploy_enabled ? module.websocket_connect_lambda[0].function_name : ""
    WEBSOCKET_DEFAULT_FUNCTION_NAME    = local.websocket_lambda_deploy_enabled ? module.websocket_default_lambda[0].function_name : ""
    WEBSOCKET_DISCONNECT_FUNCTION_NAME = local.websocket_lambda_deploy_enabled ? module.websocket_disconnect_lambda[0].function_name : ""
    WEBSOCKET_URL                      = module.websocket_api.websocket_url
  }
}
