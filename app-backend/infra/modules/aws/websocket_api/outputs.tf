output "api_id" {
  description = "API Gateway WebSocket API ID."
  value       = aws_apigatewayv2_api.api.id
}

output "api_endpoint" {
  description = "API Gateway WebSocket API endpoint."
  value       = aws_apigatewayv2_api.api.api_endpoint
}

output "stage_name" {
  description = "WebSocket API stage name."
  value       = aws_apigatewayv2_stage.stage.name
}

output "websocket_url" {
  description = "Full WebSocket URL including stage."
  value       = "${aws_apigatewayv2_api.api.api_endpoint}/${aws_apigatewayv2_stage.stage.name}"
}

output "access_log_group_name" {
  description = "CloudWatch access log group name."
  value       = var.access_log_enabled ? module.access_logs[0].log_group_name : null
}

output "manage_connections_arn" {
  description = "IAM resource ARN for posting to connected WebSocket clients."
  value       = "arn:aws:execute-api:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:${aws_apigatewayv2_api.api.id}/${aws_apigatewayv2_stage.stage.name}/POST/@connections/*"
}

output "connections_table_name" {
  description = "DynamoDB table name for active WebSocket connections."
  value       = module.connections.table_name
}

output "connections_table_arn" {
  description = "DynamoDB table ARN for active WebSocket connections."
  value       = module.connections.table_arn
}

output "connections_room_id_index_name" {
  description = "DynamoDB index name for querying WebSocket connections by room ID."
  value       = local.room_id_index_name
}

output "connections_user_id_index_name" {
  description = "DynamoDB index name for querying WebSocket connections by user ID."
  value       = local.user_id_index_name
}

output "connections_ttl_attribute_name" {
  description = "DynamoDB TTL attribute name for stale WebSocket connections."
  value       = var.connections_ttl_attribute_name
}

output "lambda_route_keys" {
  description = "WebSocket route keys integrated with Lambda functions."
  value       = keys(var.lambda_routes)
}
