# CloudWatch Log Group Module

Creates a CloudWatch log group with retention and optional KMS encryption.

Use this module when another resource needs a dedicated log group, such as API Gateway access logs or future Lambda logs.

## Resources

- `aws_cloudwatch_log_group.log_group`

## Inputs

| Name | Description |
| --- | --- |
| `name` | CloudWatch log group name. |
| `retention_in_days` | CloudWatch log retention in days. |
| `kms_key_id` | Optional KMS key ID or ARN for log group encryption. |

## Outputs

| Name | Description |
| --- | --- |
| `log_group_name` | CloudWatch log group name. |
| `log_group_arn` | CloudWatch log group ARN. |
