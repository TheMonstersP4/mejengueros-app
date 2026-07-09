output "rule_name" {
  description = "EventBridge rule name."
  value       = aws_cloudwatch_event_rule.schedule.name
}

output "rule_arn" {
  description = "EventBridge rule ARN."
  value       = aws_cloudwatch_event_rule.schedule.arn
}

output "target_id" {
  description = "EventBridge target ID."
  value       = aws_cloudwatch_event_target.lambda.target_id
}

output "failed_invocations_alarm_name" {
  description = "CloudWatch alarm name for EventBridge failed invocations."
  value       = aws_cloudwatch_metric_alarm.failed_invocations.alarm_name
}
