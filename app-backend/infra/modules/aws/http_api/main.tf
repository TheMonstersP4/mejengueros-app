resource "aws_apigatewayv2_api" "api" {
  name          = "${var.name_prefix}-http"
  protocol_type = "HTTP"

  dynamic "cors_configuration" {
    for_each = length(var.cors_allowed_origins) > 0 ? [1] : []

    content {
      allow_credentials = false
      allow_headers     = var.cors_allowed_headers
      allow_methods     = var.cors_allowed_methods
      allow_origins     = var.cors_allowed_origins
      max_age           = var.cors_max_age_seconds
    }
  }
}

module "access_logs" {
  source = "../cloudwatch_log_group"
  count  = var.access_log_enabled ? 1 : 0

  name              = "/aws/apigateway/${var.name_prefix}-http"
  retention_in_days = var.access_log_retention_days
}

resource "aws_apigatewayv2_integration" "lambda" {
  api_id                 = aws_apigatewayv2_api.api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = var.lambda_invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "lambda" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = var.route_key
  target    = "integrations/${aws_apigatewayv2_integration.lambda.id}"
}

resource "aws_apigatewayv2_stage" "stage" {
  api_id      = aws_apigatewayv2_api.api.id
  name        = var.stage_name
  auto_deploy = var.auto_deploy

  dynamic "access_log_settings" {
    for_each = var.access_log_enabled ? [1] : []

    content {
      destination_arn = module.access_logs[0].log_group_arn
      format = jsonencode({
        requestId        = "$context.requestId"
        routeKey         = "$context.routeKey"
        status           = "$context.status"
        integrationError = "$context.integrationErrorMessage"
      })
    }
  }
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowHttpApiInvoke"
  action        = "lambda:InvokeFunction"
  function_name = var.lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.api.execution_arn}/*/*"
}
