# Lambda Function Module

Creates a Lambda function from either a zip package or an ECR image.

This module expects IAM roles and CloudWatch log groups to be created outside the module.

## Resources

- `aws_lambda_function.function`

## Inputs

| Name | Description |
| --- | --- |
| `function_name` | Lambda function name. |
| `role_arn` | IAM role ARN used by the Lambda function. |
| `handler` | Lambda handler for zip package deployments. |
| `runtime` | Lambda runtime for zip package deployments. |
| `filename` | Path to the Lambda deployment package for zip deployments. |
| `source_code_hash` | Base64-encoded hash of the Lambda deployment package. |
| `image_uri` | ECR image URI for image-based Lambda deployments. |
| `package_type` | Lambda package type: Zip or Image. |
| `timeout` | Lambda timeout in seconds. |
| `memory_size` | Lambda memory size in MB. |
| `environment_variables` | Lambda environment variables. |
| `log_group_name` | Optional CloudWatch log group name created outside this module. |

## Outputs

| Name | Description |
| --- | --- |
| `function_name` | Lambda function name. |
| `function_arn` | Lambda function ARN. |
| `invoke_arn` | Lambda invoke ARN. |
