# ECR Repository Module

Creates an encrypted ECR repository with image scanning and a lifecycle policy.

## Resources

- `aws_ecr_repository.repository`
- `aws_ecr_lifecycle_policy.keep_recent`

## Inputs

| Name | Description |
| --- | --- |
| `repository_name` | ECR repository name. |
| `image_tag_mutability` | Image tag mutability. |
| `scan_on_push` | Scans images when pushed. |
| `keep_last_images` | Number of recent images to keep. |

## Outputs

| Name | Description |
| --- | --- |
| `repository_name` | ECR repository name. |
| `repository_arn` | ECR repository ARN. |
| `repository_url` | ECR repository URL. |
