# Network Module

Creates a small VPC with two private subnets. It does not create NAT gateways, public subnets, EC2 instances, ECS services, or VPN resources.

## Resources

- `aws_vpc.vpc`
- `aws_subnet.private`

## Inputs

| Name | Description |
| --- | --- |
| `name_prefix` | Prefix used for network resource names. |
| `vpc_cidr` | CIDR block for the VPC. |

## Outputs

| Name | Description |
| --- | --- |
| `vpc_id` | VPC ID. |
| `vpc_cidr` | VPC CIDR block. |
| `private_subnet_ids` | Private subnet IDs. |
