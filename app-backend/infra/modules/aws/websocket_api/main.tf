data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

locals {
  connections_table_name = var.connections_table_name != "" ? var.connections_table_name : "${var.name_prefix}-ws-connections"
  room_id_index_name     = "byRoomId"
  user_id_index_name     = "byUserId"
}

resource "aws_apigatewayv2_api" "api" {
  name                       = "${var.name_prefix}-ws"
  protocol_type              = "WEBSOCKET"
  route_selection_expression = var.route_selection_expression
}

module "access_logs" {
  source = "../cloudwatch_log_group"
  count  = var.access_log_enabled ? 1 : 0

  name              = "/aws/apigateway/${var.name_prefix}-ws"
  retention_in_days = var.access_log_retention_days
}

module "connections" {
  source = "../dynamodb_table"

  table_name   = local.connections_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "connectionId"

  attributes = [
    {
      name = "connectionId"
      type = "S"
    },
    {
      name = "roomId"
      type = "S"
    },
    {
      name = "userId"
      type = "S"
    }
  ]

  global_secondary_indexes = [
    {
      name            = local.user_id_index_name
      hash_key        = "userId"
      projection_type = "ALL"
    },
    {
      name            = local.room_id_index_name
      hash_key        = "roomId"
      projection_type = "ALL"
    }
  ]

  ttl_attribute_name             = var.connections_ttl_attribute_name
  point_in_time_recovery_enabled = var.connections_point_in_time_recovery_enabled
}

resource "aws_apigatewayv2_integration" "lambda" {
  for_each = var.lambda_routes

  api_id                 = aws_apigatewayv2_api.api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = each.value.invoke_arn
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "lambda" {
  for_each = var.lambda_routes

  api_id    = aws_apigatewayv2_api.api.id
  route_key = each.key
  target    = "integrations/${aws_apigatewayv2_integration.lambda[each.key].id}"
}

resource "aws_lambda_permission" "api_gateway" {
  for_each = var.lambda_routes

  statement_id  = "AllowExecutionFromWebSocketApi-${replace(each.key, "$", "")}"
  action        = "lambda:InvokeFunction"
  function_name = each.value.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.api.execution_arn}/*/${each.key}"
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
        eventType        = "$context.eventType"
        connectionId     = "$context.connectionId"
        status           = "$context.status"
        integrationError = "$context.integrationErrorMessage"
      })
    }
  }
}

resource "aws_apigatewayv2_route" "default" {
  count = var.default_route_enabled && !contains(keys(var.lambda_routes), "$default") ? 1 : 0

  api_id    = aws_apigatewayv2_api.api.id
  route_key = "$default"
}
