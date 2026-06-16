---
name: aws-serverless-terraform
description: Review and build AWS serverless Terraform for Lambda, API Gateway, DynamoDB, IAM, CloudWatch, S3, ECR, Cognito, and low-cost networking. Use when changing infra modules, root composition, deployment variables, or cost/security tradeoffs.
---

# AWS Serverless Terraform

Use this skill for Terraform changes around AWS serverless infrastructure.

## Rules

- Prefer small base modules for reusable resources and composition modules for product runtimes.
- Keep names as `project-env-resource`.
- Avoid NAT gateways by default; keep Lambdas outside VPC unless they must reach private resources.
- If a Lambda is attached to private subnets, document how it reaches public services or add the needed VPC endpoints.
- Use IAM least privilege and include index ARNs for DynamoDB query patterns.
- Prefer DynamoDB `Query` over `Scan` for runtime paths.
- Put CloudWatch log groups in Terraform so retention is explicit.
- Add Terraform preconditions for incompatible runtime states.
- Move large inline scripts or policies to templates when they make root modules noisy.

## Validation

Run:

```powershell
terraform fmt -recursive infra
terraform -chdir=infra/root validate
terraform -chdir=infra/root plan -var-file '..\env\dev.tfvars' -input=false -refresh=false
```
