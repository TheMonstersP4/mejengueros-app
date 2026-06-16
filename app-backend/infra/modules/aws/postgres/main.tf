resource "random_password" "master" {
  count = var.master_password == null ? 1 : 0

  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

locals {
  master_password     = var.master_password != null ? var.master_password : random_password.master[0].result
  final_snapshot_name = var.skip_final_snapshot ? null : "${var.name_prefix}-postgres-final"
}

# Storage encryption stays on by default.
resource "aws_db_instance" "database" {
  identifier = "${var.name_prefix}-postgres"

  engine                 = "postgres"
  engine_version         = var.engine_version == "" ? null : var.engine_version
  instance_class         = var.instance_class
  allocated_storage      = var.allocated_storage
  max_allocated_storage  = var.max_allocated_storage == 0 ? null : var.max_allocated_storage
  db_name                = var.db_name
  username               = var.master_username
  password               = local.master_password
  port                   = var.port
  db_subnet_group_name   = var.db_subnet_group_name
  vpc_security_group_ids = var.security_group_ids
  publicly_accessible    = var.publicly_accessible
  storage_encrypted      = true

  backup_retention_period   = var.backup_retention_period
  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = var.skip_final_snapshot
  final_snapshot_identifier = local.final_snapshot_name

  auto_minor_version_upgrade = true
  copy_tags_to_snapshot      = true
}
