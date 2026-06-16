# Skills

Project skills live here so repository agents can reuse the same workflow rules.

## Structure

```text
skills/
  <skill-name>/
    SKILL.md
    agents/
      openai.yaml
    references/
      *.md
```

## Rules

- `SKILL.md` contains the trigger description and the short workflow.
- `agents/openai.yaml` contains UI metadata.
- `references/` contains longer standards loaded only when needed.
- Do not add generated output, secrets, local notes, or temporary files.

## Current Skills

- `conventional-commits`: split changes and write Conventional Commit messages.
- `nestjs-ddd-solid`: build and review NestJS code with DDD, SOLID, Prisma, Cognito, Fastify, and Pino.
- `repo-documentation`: apply TSDoc and Terraform documentation rules.
- `unit-testing`: write and review Jest unit tests with coverage discipline.
- `aws-serverless-terraform`: review AWS serverless Terraform for Lambda, API Gateway, DynamoDB, IAM, CloudWatch, cost, and VPC tradeoffs.
- `github-actions-deploy`: review GitHub Actions deploy pipelines, OIDC, quality gates, and script-based deploys.
- `security-review`: review secrets, OAuth, Cognito, IAM, logs, buckets, and public exposure.
