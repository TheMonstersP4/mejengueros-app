# GitHub OIDC Deploy Module

Creates an IAM role for GitHub Actions direct deploys without static AWS keys.

## Resources

- `aws_iam_openid_connect_provider.github_actions` when no existing provider ARN is supplied
- `aws_iam_role.github_actions`
- `aws_iam_role_policy.direct_deploy`

## Inputs

| Name | Description |
| --- | --- |
| `name_prefix` | Prefix used in IAM resource names. |
| `repository` | Primary GitHub repository in owner/name format. Kept for compatibility. |
| `repositories` | GitHub repositories in owner/name format allowed to assume the deploy role. |
| `branch` | Git branch allowed to assume the role. |
| `environment` | Optional GitHub environment name allowed to assume the role. |
| `existing_oidc_provider_arn` | Existing GitHub OIDC provider ARN. |
| `poc_site_bucket_name` | S3 bucket name allowed for POC site deploy. |
| `ecr_repository_arn` | ECR repository ARN allowed for Docker pushes. |
| `lambda_function_arns` | Lambda function ARNs allowed for code updates. |

## Outputs

| Name | Description |
| --- | --- |
| `role_arn` | IAM role ARN for GitHub Actions OIDC. |
| `role_name` | IAM role name for GitHub Actions OIDC. |
