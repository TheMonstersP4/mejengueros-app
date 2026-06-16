# PostgreSQL Module

Creates an encrypted RDS PostgreSQL instance using provided networking resources.

## Resources

- `random_password.master`
- `aws_db_instance.database`

## Inputs

| Name | Description |
| --- | --- |
| `name_prefix` | Prefix used for PostgreSQL resources. |
| `db_name` | Initial database name. |
| `master_username` | Master username. |
| `master_password` | Optional master password. If null, Terraform creates one. |
| `instance_class` | RDS instance class. |
| `allocated_storage` | Initial storage in GB. |
| `max_allocated_storage` | Maximum autoscaled storage in GB. |
| `engine_version` | PostgreSQL engine version. |
| `port` | PostgreSQL port. |
| `publicly_accessible` | Whether the DB has a public endpoint. |
| `deletion_protection` | Protects DB from deletion. |
| `skip_final_snapshot` | Skips final snapshot on destroy. |
| `backup_retention_period` | Backup retention in days. |
| `db_subnet_group_name` | DB subnet group name for the PostgreSQL instance. |
| `security_group_ids` | Security group IDs attached to the PostgreSQL instance. |

## Outputs

| Name | Description |
| --- | --- |
| `db_instance_id` | RDS instance identifier. |
| `endpoint` | RDS endpoint. |
| `port` | RDS port. |
| `db_name` | Database name. |
| `security_group_id` | First security group attached to PostgreSQL. |
| `vpc_id` | Deprecated. |
| `subnet_ids` | Deprecated. |
| `master_password` | Master password. |
