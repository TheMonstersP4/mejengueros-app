# API Gateway Account CloudWatch Module

Configures the regional API Gateway account setting used to push access logs to CloudWatch.

This is an account-and-region setting. Use it once per AWS account and region to avoid multiple Terraform stacks fighting over the same API Gateway account configuration.

## Resources

- `aws_iam_role.cloudwatch`
- `aws_iam_role_policy_attachment.cloudwatch`
- `aws_api_gateway_account.cloudwatch`

## Inputs

| Name | Description |
| --- | --- |
| `name_prefix` | Prefix used for the API Gateway CloudWatch IAM role. |

## Outputs

| Name | Description |
| --- | --- |
| `cloudwatch_role_arn` | IAM role ARN configured as the regional API Gateway CloudWatch role. |
| `cloudwatch_role_name` | IAM role name configured as the regional API Gateway CloudWatch role. |
