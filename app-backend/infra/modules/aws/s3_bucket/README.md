# S3 Bucket Module

Creates a private encrypted S3 bucket for application files.

## Resources

- `random_id.suffix`
- `aws_s3_bucket.bucket`
- `aws_s3_bucket_public_access_block.public_access_block`
- `aws_s3_bucket_ownership_controls.ownership_controls`
- `aws_s3_bucket_server_side_encryption_configuration.encryption`
- `aws_s3_bucket_versioning.versioning`

## Inputs

| Name | Description |
| --- | --- |
| `name_prefix` | Prefix used when generating the bucket name. |
| `bucket_name` | Optional fixed bucket name. |
| `purpose` | Short purpose suffix used when bucket_name is empty. |
| `versioning` | Enables bucket versioning. |
| `force_destroy` | Allows Terraform to delete non-empty buckets. |

## Outputs

| Name | Description |
| --- | --- |
| `bucket_name` | S3 bucket name. |
| `bucket_arn` | S3 bucket ARN. |
| `bucket_regional_domain_name` | S3 regional domain name. |
