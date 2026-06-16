# WebSocket API Module

Creates an API Gateway v2 WebSocket API with a stage, optional Lambda route integrations, optional access logs, and a DynamoDB table for active connections.

This is a composed module. It uses base modules for CloudWatch logs and DynamoDB so small projects can use one module, while larger projects can still reuse the base modules directly.

## Resources

- `aws_apigatewayv2_api.api`
- `aws_apigatewayv2_integration.lambda`
- `aws_apigatewayv2_route.lambda`
- `aws_apigatewayv2_stage.stage`
- `aws_apigatewayv2_route.default`
- `aws_lambda_permission.api_gateway`
- `module.access_logs`
- `module.connections`

## Inputs

| Name | Description |
| --- | --- |
| `name_prefix` | Prefix used for WebSocket API resources. |
| `route_selection_expression` | JSON expression API Gateway uses to choose WebSocket routes. |
| `stage_name` | WebSocket API stage name. |
| `auto_deploy` | Automatically deploy changes to the WebSocket API stage. |
| `access_log_enabled` | Enables CloudWatch access logs for the WebSocket stage. |
| `access_log_retention_days` | CloudWatch log retention in days for WebSocket access logs. |
| `default_route_enabled` | Creates a default route without integration. |
| `lambda_routes` | Lambda integrations keyed by WebSocket route key. |
| `connections_table_name` | Optional fixed DynamoDB table name for WebSocket connections. |
| `connections_ttl_attribute_name` | DynamoDB TTL attribute name for stale WebSocket connections. |
| `connections_point_in_time_recovery_enabled` | Enables point-in-time recovery for the connections table. |

## Outputs

| Name | Description |
| --- | --- |
| `api_id` | API Gateway WebSocket API ID. |
| `api_endpoint` | API Gateway WebSocket API endpoint. |
| `stage_name` | WebSocket API stage name. |
| `websocket_url` | Full WebSocket URL including stage. |
| `access_log_group_name` | CloudWatch access log group name. |
| `manage_connections_arn` | IAM resource ARN for posting to connected WebSocket clients. |
| `connections_table_name` | DynamoDB table name for active WebSocket connections. |
| `connections_table_arn` | DynamoDB table ARN for active WebSocket connections. |
| `connections_room_id_index_name` | DynamoDB index name for querying connections by room ID. |
| `connections_user_id_index_name` | DynamoDB index name for querying connections by user ID. |
| `connections_ttl_attribute_name` | DynamoDB TTL attribute name for stale WebSocket connections. |
| `lambda_route_keys` | WebSocket route keys integrated with Lambda functions. |
