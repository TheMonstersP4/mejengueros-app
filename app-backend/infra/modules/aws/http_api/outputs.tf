output "api_id" {
  description = "HTTP API ID."
  value       = aws_apigatewayv2_api.api.id
}

output "api_endpoint" {
  description = "HTTP API endpoint."
  value       = aws_apigatewayv2_api.api.api_endpoint
}

output "stage_name" {
  description = "HTTP API stage name."
  value       = aws_apigatewayv2_stage.stage.name
}

output "access_log_group_name" {
  description = "CloudWatch access log group name."
  value       = var.access_log_enabled ? module.access_logs[0].log_group_name : null
}
