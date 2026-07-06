locals {
  failed_invocations_alarm_name = var.failed_invocations_alarm_name != "" ? var.failed_invocations_alarm_name : "${var.name}-eventbridge-failures"
}

resource "aws_cloudwatch_event_rule" "schedule" {
  name                = var.name
  description         = var.description
  schedule_expression = var.schedule_expression
}

resource "aws_cloudwatch_event_target" "lambda" {
  rule      = aws_cloudwatch_event_rule.schedule.name
  target_id = var.target_id
  arn       = var.lambda_function_arn
}

resource "aws_lambda_permission" "eventbridge" {
  statement_id  = var.permission_statement_id
  action        = "lambda:InvokeFunction"
  function_name = var.lambda_function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.schedule.arn
}

resource "aws_cloudwatch_metric_alarm" "failed_invocations" {
  alarm_name          = local.failed_invocations_alarm_name
  alarm_description   = "EventBridge target reported failed invocations."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "FailedInvocations"
  namespace           = "AWS/Events"
  period              = 300
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_actions       = var.failed_invocations_alarm_actions
  ok_actions          = var.failed_invocations_alarm_actions

  dimensions = {
    RuleName = aws_cloudwatch_event_rule.schedule.name
  }
}
