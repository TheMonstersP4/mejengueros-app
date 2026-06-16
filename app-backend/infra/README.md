# Infrastructure

Backend infrastructure modules:

- Cognito user pool with optional Google and Microsoft identity providers.
- Private network without NAT gateway, EC2, or VPS.
- Optional security group, DB subnet group, and RDS PostgreSQL.
- S3 application bucket.
- ECR repository for Docker images.
- HTTP API Gateway with optional Lambda container integration.
- API Gateway WebSocket API with optional Lambda routes, access logs, and DynamoDB connection table.
- API Gateway account CloudWatch role when access logs are enabled.

PostgreSQL is private when `postgres_enabled = true`; it is disabled by default so early environments do not pay for RDS. S3 blocks public access, and examples avoid real secrets. The network module does not create a NAT gateway because NAT adds a fixed monthly cost.

Google and Microsoft login are configured in Cognito from OAuth client credentials. Terraform can create the Microsoft Entra app registration when `azuread_enabled = true`; Google OAuth still needs to be created in Google Cloud and passed through `google_client_id` and `google_client_secret`.

The HTTP API Lambda can run inside private subnets to reach RDS without NAT. If that Lambda needs public internet access from the VPC, add VPC endpoints where possible or add NAT only when the cost is accepted.

## Commands

```powershell
terraform -chdir=infra/root init
terraform -chdir=infra/root fmt -recursive
terraform -chdir=infra/root validate
terraform -chdir=infra/root plan '-var-file=../env/dev.tfvars'
```

## Environments

Use files under `infra/env` for environment-specific values. Commit only `.example` files.

```text
dev.tfvars.example
prod.tfvars.example
dev.backend.hcl.example
prod.backend.hcl.example
```
