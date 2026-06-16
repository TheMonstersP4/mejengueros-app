# S3 Static Site Module

Creates a public S3 website bucket for static POC assets.

## Resources

- `aws_s3_bucket.site`
- `aws_s3_bucket_website_configuration.site`
- `aws_s3_bucket_policy.public_read`
- `aws_s3_bucket_public_access_block.site`
- `aws_s3_bucket_ownership_controls.site`
- `aws_s3_bucket_server_side_encryption_configuration.site`

## Inputs

| Name | Description |
| --- | --- |
| `bucket_name` | S3 bucket name. For S3 website custom domains, use the full domain name. |
| `force_destroy` | Allows Terraform to delete a non-empty bucket. |

## Outputs

| Name | Description |
| --- | --- |
| `bucket_name` | S3 static site bucket name. |
| `bucket_arn` | S3 static site bucket ARN. |
| `website_endpoint` | S3 website endpoint. |
| `website_domain` | S3 website domain for DNS CNAME records. |
