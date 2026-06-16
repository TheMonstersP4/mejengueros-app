# DynamoDB Table Module

Creates a DynamoDB table with optional TTL and global secondary indexes.

## Resources

- `aws_dynamodb_table.table`

## Inputs

| Name | Description |
| --- | --- |
| `table_name` | DynamoDB table name. |
| `billing_mode` | DynamoDB billing mode. |
| `hash_key` | DynamoDB table hash key. |
| `range_key` | Optional DynamoDB table range key. |
| `attributes` | DynamoDB table attributes used by keys and indexes. |
| `global_secondary_indexes` | DynamoDB global secondary indexes. |
| `ttl_attribute_name` | Optional DynamoDB TTL attribute name. |
| `point_in_time_recovery_enabled` | Enables point-in-time recovery. |

## Outputs

| Name | Description |
| --- | --- |
| `table_name` | DynamoDB table name. |
| `table_arn` | DynamoDB table ARN. |
| `table_id` | DynamoDB table ID. |
