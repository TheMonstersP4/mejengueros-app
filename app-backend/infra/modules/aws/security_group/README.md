# Security Group Module

Creates a security group with configurable ingress and egress rules.

## Resources

- `aws_security_group.security_group`
- `aws_security_group_rule.ingress`
- `aws_security_group_rule.egress`

## Inputs

| Name | Description |
| --- | --- |
| `name` | Security group name. |
| `description` | Security group description. |
| `vpc_id` | VPC ID where the security group will be created. |
| `ingress_rules` | Ingress rules for the security group. |
| `egress_rules` | Egress rules for the security group. |

## Outputs

| Name | Description |
| --- | --- |
| `security_group_id` | Security group ID. |
| `security_group_arn` | Security group ARN. |
| `security_group_name` | Security group name. |
