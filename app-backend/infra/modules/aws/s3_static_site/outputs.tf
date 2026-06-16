output "bucket_name" {
  description = "S3 static site bucket name."
  value       = aws_s3_bucket.site.bucket
}

output "bucket_arn" {
  description = "S3 static site bucket ARN."
  value       = aws_s3_bucket.site.arn
}

output "website_endpoint" {
  description = "S3 website endpoint."
  value       = aws_s3_bucket_website_configuration.site.website_endpoint
}

output "website_domain" {
  description = "S3 website domain for DNS CNAME records."
  value       = aws_s3_bucket_website_configuration.site.website_domain
}
