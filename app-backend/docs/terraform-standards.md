# Terraform Standards

These rules keep the infrastructure predictable as the project grows.

## Module Boundaries

- `infra/root` composes infrastructure for one environment.
- `infra/modules/aws/*` contains reusable building blocks.
- A module should do one job: network, security group, DB subnet group, Cognito, RDS PostgreSQL, S3, ECR, CloudWatch log group, WebSocket API, DynamoDB table, etc.
- App-level modules can be added later when several AWS pieces need to move together.

Use two module levels:

- Base modules wrap one AWS concern and stay reusable across projects.
- Composed modules wire base modules into a ready-to-use capability.

Examples:

```text
Base:     cloudwatch_log_group, dynamodb_table, security_group
Composed: websocket_api = API Gateway WebSocket + log group + connections table
```

For small projects, use composed modules from `root`. For larger projects, use base modules directly when custom wiring is needed.

## Files

Each module should normally have:

```text
main.tf
variables.tf
outputs.tf
versions.tf
README.md
```

Keep provider configuration in the root module unless a module truly needs its own provider alias.

Documentation pattern:

- `versions.tf`: no comments, only Terraform and provider constraints.
- `variables.tf`: every variable must include `description`.
- `main.tf`: comments only for cost, security, or behavior that is not obvious from the resource name.
- `outputs.tf`: every output must include `description`.
- `README.md`: generated with `terraform-docs` when available, or written by hand using the same sections.

Recommended README sections:

```text
# Module Name

Short purpose statement.

## Resources
## Inputs
## Outputs
```

When `terraform-docs` is available, run:

```powershell
terraform-docs markdown table infra/modules/aws/<module-name> > infra/modules/aws/<module-name>/README.md
```

Do not add comments that repeat the resource type. For example, avoid `# Creates an S3 bucket` above `aws_s3_bucket`.

## Naming

Resources use this pattern:

```text
<project>-<env>-<purpose>
```

Example:

```text
school-dev-auth
school-dev-postgres
school-dev-http
school-dev-ws
```

Use the resource purpose, not the AWS service name, when that is clearer for
humans. For example, an ECR repository that stores the main project image can
be named `school-dev`; a second image later can use `school-dev-worker`.

Allow explicit name overrides only for provider constraints such as global
uniqueness, migration from existing infrastructure, or organization-wide naming
rules.

## Secrets

- Real `.tfvars` files stay local.
- `.tfvars.example` files are templates.
- Terraform state is never committed.
- Use generated passwords or secret managers instead of hardcoded credentials.

## Cost Guardrails

- Do not add NAT gateways unless there is a clear runtime need.
- Do not add EC2, Lightsail, or ECS services until the API runtime is chosen.
- Keep PostgreSQL private by default.
- Use managed or serverless entry points for early APIs when traffic is low.

## Before Applying

Run these commands before applying changes:

```powershell
terraform -chdir=infra/root fmt -recursive
terraform -chdir=infra/root validate
terraform -chdir=infra/root plan -var-file=../env/dev.tfvars
```
