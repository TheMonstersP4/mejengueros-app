resource "aws_security_group" "security_group" {
  name        = var.name
  description = var.description
  vpc_id      = var.vpc_id

  tags = {
    Name = var.name
  }
}

resource "aws_security_group_rule" "ingress" {
  for_each = {
    for index, rule in var.ingress_rules : index => rule
  }

  type                     = "ingress"
  description              = try(each.value.description, null)
  from_port                = each.value.from_port
  to_port                  = each.value.to_port
  protocol                 = each.value.protocol
  cidr_blocks              = try(each.value.cidr_blocks, [])
  source_security_group_id = try(each.value.source_security_group_id, null)
  security_group_id        = aws_security_group.security_group.id
}

resource "aws_security_group_rule" "egress" {
  for_each = {
    for index, rule in var.egress_rules : index => rule
  }

  type                     = "egress"
  description              = try(each.value.description, null)
  from_port                = each.value.from_port
  to_port                  = each.value.to_port
  protocol                 = each.value.protocol
  cidr_blocks              = try(each.value.cidr_blocks, [])
  source_security_group_id = try(each.value.source_security_group_id, null)
  security_group_id        = aws_security_group.security_group.id
}
