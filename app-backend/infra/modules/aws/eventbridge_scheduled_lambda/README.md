# EventBridge Scheduled Lambda Module

Creates an EventBridge schedule that invokes one Lambda function.

## Resources

- `aws_cloudwatch_event_rule.schedule`
- `aws_cloudwatch_event_target.lambda`
- `aws_lambda_permission.eventbridge`
- `aws_cloudwatch_metric_alarm.failed_invocations`

## Inputs

| Name | Description |
| --- | --- |
| `name` | EventBridge rule name. |
| `description` | EventBridge rule description. |
| `schedule_expression` | EventBridge schedule expression. |
| `target_id` | EventBridge target ID. |
| `lambda_function_name` | Lambda function name allowed to be invoked by EventBridge. |
| `lambda_function_arn` | Lambda function ARN used as the EventBridge target. |
| `permission_statement_id` | Lambda permission statement ID for EventBridge invocation. |
| `failed_invocations_alarm_actions` | CloudWatch alarm action ARNs for EventBridge failed invocations. |
| `failed_invocations_alarm_name` | CloudWatch alarm name for EventBridge failed invocations. |

## Outputs

| Name | Description |
| --- | --- |
| `rule_name` | EventBridge rule name. |
| `rule_arn` | EventBridge rule ARN. |
| `target_id` | EventBridge target ID. |
| `failed_invocations_alarm_name` | CloudWatch alarm name for EventBridge failed invocations. |
