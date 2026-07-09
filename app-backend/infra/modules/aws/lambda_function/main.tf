resource "aws_lambda_function" "function" {
  function_name = var.function_name
  role          = var.role_arn
  package_type  = var.package_type
  timeout       = var.timeout
  memory_size   = var.memory_size

  handler          = var.package_type == "Zip" ? var.handler : null
  runtime          = var.package_type == "Zip" ? var.runtime : null
  filename         = var.package_type == "Zip" ? var.filename : null
  source_code_hash = var.package_type == "Zip" ? var.source_code_hash : null
  image_uri        = var.package_type == "Image" ? var.image_uri : null

  dynamic "environment" {
    for_each = length(var.environment_variables) > 0 ? [1] : []

    content {
      variables = var.environment_variables
    }
  }

  dynamic "vpc_config" {
    for_each = length(var.subnet_ids) > 0 && length(var.security_group_ids) > 0 ? [1] : []

    content {
      subnet_ids         = var.subnet_ids
      security_group_ids = var.security_group_ids
    }
  }

  lifecycle {
    # Deployment pipelines own ZIP code updates after Terraform creates the function.
    ignore_changes = [
      filename,
      image_uri,
      source_code_hash
    ]
  }
}
