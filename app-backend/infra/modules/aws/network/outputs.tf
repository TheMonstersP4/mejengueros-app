output "vpc_id" {
  description = "VPC ID."
  value       = aws_vpc.vpc.id
}

output "vpc_cidr" {
  description = "VPC CIDR block."
  value       = aws_vpc.vpc.cidr_block
}

output "private_subnet_ids" {
  description = "Private subnet IDs."
  value       = aws_subnet.private[*].id
}
