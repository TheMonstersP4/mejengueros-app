# HTTP API Module

Creates an API Gateway HTTP API integrated with one Lambda function.

Use this for the main NestJS HTTP API when it is deployed as a Lambda container image.

## Resources

- `aws_apigatewayv2_api.api`
- `aws_apigatewayv2_integration.lambda`
- `aws_apigatewayv2_route.default`
- `aws_apigatewayv2_stage.stage`
- `aws_lambda_permission.api_gateway`
- `module.access_logs`

## Inputs

| Name | Description |
| --- | --- |
| `name_prefix` | Prefix used for HTTP API resources. |
| `stage_name` | HTTP API stage name. |
| `lambda_invoke_arn` | Lambda invoke ARN used by API Gateway. |
| `lambda_function_name` | Lambda function name allowed to receive API Gateway invocations. |
| `route_key` | HTTP API route key. |
| `auto_deploy` | Automatically deploy changes to the HTTP API stage. |
| `access_log_enabled` | Enables CloudWatch access logs for the HTTP API stage. |
| `access_log_retention_days` | CloudWatch log retention in days for HTTP API access logs. |

## Outputs

| Name | Description |
| --- | --- |
| `api_id` | HTTP API ID. |
| `api_endpoint` | HTTP API endpoint. |
| `stage_name` | HTTP API stage name. |
| `access_log_group_name` | CloudWatch access log group name. |
