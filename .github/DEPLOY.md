# Deploy

Deploys are intentionally script-driven. GitHub Actions only handles checkout, OIDC auth, and job routing.

API and WebSocket deploy jobs depend on the API quality gate: lint, unit tests, and build must pass before code is published.

## GitHub Secrets

Create these secrets in the `dev` environment:

```text
AWS_ROLE_ARN_DEV
DATABASE_URL
DEPLOY_DEV_CONFIG
```

Terraform must be applied with `github_repository = "TheMonstersP4/mejengueros-app"` so the AWS OIDC role trusts this repository.

`AWS_ROLE_ARN_DEV` comes from:

```powershell
terraform -chdir=app-backend/infra/root output -raw github_actions_iam_role_arn
```

`DEPLOY_DEV_CONFIG` comes from:

```powershell
terraform -chdir=app-backend/infra/root output -json deploy_config
```

`DATABASE_URL` is the PostgreSQL connection string used by Prisma. For the
shared Azure database, keep `schema=mejengueros_dev` in the query string.

## Scripts

- `.github/scripts/deploy-api-image.sh`: updates the database secret when available, builds `app-backend/api/Dockerfile`, pushes to ECR, and updates the HTTP Lambda image.
- `.github/scripts/run-api-migrations.sh`: runs Prisma migrations when `DATABASE_URL` is configured.
- `.github/scripts/package-websocket-lambdas.sh`: builds the API package and creates `app-backend/api/.lambda/websocket.zip`.
- `.github/scripts/deploy-websocket-lambdas.sh`: updates the three WebSocket route Lambdas from the zip.
- `.github/scripts/deploy-poc-site.sh`: syncs `app-backend/poc/web-chat` to S3.

## Bootstrap Order

1. Apply Terraform with ECR and POC site enabled.
2. Store `AWS_ROLE_ARN_DEV` and `DEPLOY_DEV_CONFIG` in GitHub.
3. Store `DATABASE_URL` in the `dev` GitHub environment if the API should use PostgreSQL.
4. Push to `main`.
5. The first API deploy pushes `ECR_REPOSITORY_URI:latest` even if the HTTP Lambda does not exist yet.
6. Enable HTTP API Lambda in Terraform with `api_http_enabled = true`.
7. Apply Terraform and refresh `DEPLOY_DEV_CONFIG`.

`API_LAMBDA_FUNCTION_NAME` is deterministic, for example `mejengueros-dev-http`. During bootstrap the script still pushes the Docker image to ECR. If the Lambda does not exist yet, it skips only the Lambda update step.

When the HTTP API is ready:

1. Run the GitHub deploy once so `ECR_REPOSITORY_URI:latest` exists.
2. Set `api_http_enabled = true`.
3. Leave `api_lambda_image_tag = "latest"` unless you need a pinned tag.
4. Apply Terraform.
5. Refresh `DEPLOY_DEV_CONFIG` from Terraform output and update the GitHub secret.
