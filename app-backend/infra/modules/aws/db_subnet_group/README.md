# DB Subnet Group Module

Creates an RDS DB subnet group.

## Resources

- `aws_db_subnet_group.subnet_group`

## Inputs

| Name | Description |
| --- | --- |
| `name` | DB subnet group name. |
| `subnet_ids` | Subnet IDs for the DB subnet group. |

## Outputs

| Name | Description |
| --- | --- |
| `db_subnet_group_name` | DB subnet group name. |
| `db_subnet_group_arn` | DB subnet group ARN. |
