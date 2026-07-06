# Root Module

Composes the environment infrastructure from reusable AWS modules.

API Gateway account logging is an AWS account-and-region setting, not a per-API resource. Keep this root module as the owner for that setting in each environment.

Cloudflare stays optional. Terraform still configures the provider during validation, so an empty token uses a harmless placeholder when all Cloudflare resources are disabled.

## Resources

- `module.network`
- `module.cognito`
- `module.postgres_security_group` when PostgreSQL is enabled
- `module.postgres_subnet_group` when PostgreSQL is enabled
- `module.postgres` when PostgreSQL is enabled
- `module.app_bucket`
- `module.ecr_api`
- `module.microsoft_oauth`
- `module.api_gateway_account_cloudwatch` when API Gateway access logs are enabled
- `module.api_lambda`
- `module.http_api`
- `module.reservation_completion_worker_lambda` when the reservation completion worker is enabled
- `module.websocket_api`

## Inputs

| Name | Description |
| --- | --- |
| `project` | Project name used in resource names. |
| `env` | Environment name. |
| `aws_region` | AWS region for this environment. |
| `owner` | Owner tag value. |
| `network_vpc_cidr` | CIDR block for the project VPC. |
| `cognito_mfa_configuration` | Cognito MFA mode. |
| `cognito_password_minimum_length` | Minimum password length for Cognito users. |
| `cognito_callback_urls` | Allowed OAuth callback URLs. |
| `cognito_logout_urls` | Allowed OAuth logout URLs. |
| `cognito_admin_users` | Optional bootstrap admin users. |
| `google_enabled` | Enables Google login. |
| `google_client_id` | Google OAuth client ID. |
| `google_client_secret` | Google OAuth client secret. |
| `microsoft_tenant_id` | Microsoft authority tenant value. Use `9188040d-6c67-4c5b-b112-36a304b66dad` for personal Outlook, Hotmail, Live, and Skype accounts. |
| `microsoft_provider_name` | Cognito provider name for Microsoft. |
| `azuread_enabled` | Creates the Microsoft Entra OAuth app and passes it to Cognito. |
| `azuread_sign_in_audience` | Supported Microsoft account audience for the generated app registration. |
| `azuread_homepage_url` | Optional homepage URL for the generated app registration. |
| `azuread_logout_url` | Optional logout URL for the generated app registration. |
| `azuread_owner_object_ids` | Optional Microsoft Entra object IDs that own the generated app registration. |
| `azuread_client_secret_display_name` | Display name for the generated Microsoft Entra client secret. |
| `azuread_client_secret_end_date` | Optional Microsoft Entra client secret expiration date. |
| `postgres_enabled` | Creates PostgreSQL RDS resources. |
| `postgres_db_name` | Initial PostgreSQL database name. |
| `postgres_master_username` | PostgreSQL master username. |
| `postgres_master_password` | Optional PostgreSQL master password. |
| `postgres_instance_class` | RDS instance class. |
| `postgres_allocated_storage` | Initial RDS storage in GB. |
| `postgres_max_allocated_storage` | Maximum autoscaled RDS storage in GB. |
| `postgres_engine_version` | PostgreSQL engine version. |
| `postgres_publicly_accessible` | Whether the RDS instance gets a public endpoint. |
| `postgres_allowed_cidr_blocks` | CIDR blocks allowed to connect to PostgreSQL. |
| `postgres_deletion_protection` | Protects the RDS instance from deletion. |
| `postgres_skip_final_snapshot` | Skips final snapshot on DB destroy. |
| `postgres_backup_retention_period` | RDS backup retention in days. |
| `s3_bucket_name` | Optional fixed S3 bucket name. |
| `s3_versioning` | Enables S3 bucket versioning. |
| `s3_force_destroy` | Allows Terraform to delete a non-empty bucket. |
| `s3_upload_expiration_days` | Days before pending direct-upload objects expire. Use `0` to disable cleanup. |
| `ecr_image_tag_mutability` | ECR image tag mutability. |
| `ecr_scan_on_push` | Scans ECR images when pushed. |
| `ecr_keep_last_images` | Number of recent ECR images to keep. |
| `api_http_enabled` | Creates the public HTTP API and Lambda container integration. |
| `api_lambda_image_tag` | ECR image tag used by the HTTP API Lambda. The repository URL is derived from the project ECR repository. |
| `reservation_completion_worker_enabled` | Creates the scheduled reservation completion worker Lambda when a deployment zip is provided. |
| `reservation_completion_worker_package_filename` | Path to the reservation completion worker deployment zip. Leave empty to skip the worker Lambda. |
| `reservation_completion_worker_package_source_code_hash` | Optional Base64-encoded hash for the reservation completion worker deployment zip. |
| `reservation_completion_worker_runtime` | Runtime used by the reservation completion worker zip Lambda. |
| `reservation_completion_worker_memory_size` | Reservation completion worker Lambda memory size in MB. |
| `reservation_completion_worker_timeout` | Reservation completion worker Lambda timeout in seconds. |
| `reservation_completion_worker_schedule_expression` | EventBridge schedule expression for the reservation completion worker. |
| `reservation_completion_worker_log_retention_days` | CloudWatch log retention in days for the reservation completion worker Lambda. |
| `reservation_completion_worker_alarm_actions` | Optional CloudWatch alarm action ARNs for reservation completion worker failure alarms. |
| `api_lambda_vpc_enabled` | Attaches the HTTP API Lambda to private subnets. Keep false unless it must reach private RDS because this stack does not create NAT. |
| `api_database_secret_enabled` | Creates an AWS Secrets Manager secret for an external API database URL. |
| `api_database_secret_arn` | Existing AWS Secrets Manager secret ARN containing the API database URL. |
| `api_cors_allowed_origins` | Browser origins allowed to call the API and upload directly to S3. |
| `api_s3_upload_url_ttl_seconds` | Time-to-live in seconds for API-generated S3 upload URLs. |
| `api_s3_profile_image_max_bytes` | Maximum profile image upload size in bytes. |
| `api_s3_allowed_image_mime_types` | Allowed MIME types for API image uploads. |
| `websocket_route_selection_expression` | JSON expression API Gateway uses to choose WebSocket routes. |
| `websocket_stage_name` | WebSocket API stage name. |
| `websocket_auto_deploy` | Automatically deploy changes to the WebSocket API stage. |
| `websocket_access_log_enabled` | Enables CloudWatch access logs for the WebSocket API stage. |
| `websocket_log_retention_days` | CloudWatch log retention in days for WebSocket access logs. |
| `websocket_default_route_enabled` | Creates a default WebSocket route without integration. |
| `websocket_connections_table_name` | Optional fixed DynamoDB table name for WebSocket connections. |
| `websocket_connections_ttl_attribute_name` | DynamoDB TTL attribute name for stale WebSocket connections. |
| `websocket_connections_point_in_time_recovery_enabled` | Enables point-in-time recovery for the WebSocket connections table. |

## Outputs

| Name | Description |
| --- | --- |
| `cognito_user_pool_id` | Cognito user pool ID. |
| `cognito_user_pool_client_id` | Cognito app client ID. |
| `cognito_domain_url` | Cognito hosted UI base URL. |
| `cognito_identity_provider_redirect_url` | Redirect URL for Google Cloud and Microsoft Entra OAuth apps. |
| `azuread_application_client_id` | Microsoft Entra application client ID created by Terraform. |
| `azuread_application_object_id` | Microsoft Entra application object ID created by Terraform. |
| `network_vpc_id` | Project VPC ID. |
| `network_private_subnet_ids` | Private subnet IDs for internal services. |
| `postgres_endpoint` | PostgreSQL endpoint when PostgreSQL is enabled. |
| `postgres_port` | PostgreSQL port when PostgreSQL is enabled. |
| `postgres_database_name` | PostgreSQL database name when PostgreSQL is enabled. |
| `postgres_master_password` | PostgreSQL master password when PostgreSQL is enabled. |
| `s3_bucket_name` | Application S3 bucket name. |
| `s3_bucket_arn` | Application S3 bucket ARN. |
| `ecr_repository_name` | ECR repository name. |
| `ecr_repository_url` | ECR repository URL for Docker pushes. |
| `ecr_repository_arn` | ECR repository ARN. |
| `reservation_completion_worker_function_name` | Expected reservation completion worker Lambda function name. |
| `reservation_completion_worker_schedule_rule_name` | EventBridge rule name for the reservation completion worker schedule. |
| `reservation_completion_worker_lambda_error_alarm_name` | CloudWatch alarm name for reservation completion worker Lambda errors. |
| `reservation_completion_worker_failed_invocations_alarm_name` | CloudWatch alarm name for reservation completion worker EventBridge failed invocations. |
| `deploy_config.DATABASE_SECRET_ARN` | Secrets Manager ARN used by GitHub Actions and the API Lambda. |
| `deploy_config.RESERVATION_COMPLETION_WORKER_FUNCTION_NAME` | Lambda name used by GitHub Actions to deploy the reservation completion worker. |
| `websocket_api_id` | API Gateway WebSocket API ID. |
| `websocket_url` | Full WebSocket URL including stage. |
| `websocket_access_log_group_name` | CloudWatch access log group name for the WebSocket API. |
| `websocket_manage_connections_arn` | IAM resource ARN for posting to connected WebSocket clients. |
| `websocket_connections_table_name` | DynamoDB table name for active WebSocket connections. |
| `websocket_connections_table_arn` | DynamoDB table ARN for active WebSocket connections. |
| `websocket_connections_room_id_index_name` | DynamoDB index name for querying WebSocket connections by room ID. |
| `websocket_connections_user_id_index_name` | DynamoDB index name for querying WebSocket connections by user ID. |
