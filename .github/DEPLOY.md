# Deploy

Deploys are intentionally script-driven. GitHub Actions only handles checkout, OIDC auth, and job routing.

API, WebSocket, and reservation worker deploy jobs depend on the API quality gate: lint, unit tests, and build must pass before code is published. The web deploy job depends on the web quality gate: tests and production build must pass before static files are published.

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
- `.github/scripts/package-reservation-completion-worker.sh`: installs API dependencies, builds the API, and creates `app-backend/api/.lambda/reservation-completion.zip`.
- `.github/scripts/deploy-reservation-completion-worker.sh`: updates the scheduled reservation completion Lambda from the packaged zip.
- `.github/scripts/run-api-migrations.sh`: runs Prisma migrations when `DATABASE_URL` is configured.
- `.github/scripts/package-websocket-lambdas.sh`: builds the API package and creates `app-backend/api/.lambda/websocket.zip`.
- `.github/scripts/deploy-websocket-lambdas.sh`: updates the three WebSocket route Lambdas from the zip.
- `.github/scripts/deploy-web-site.sh`: builds `app-web` and syncs `app-web/dist` to S3.

## Reservation Completion Worker

Enable the worker in Terraform with `reservation_completion_worker_enabled = true` and provide the packaged zip path during Terraform apply. The GitHub deploy workflow then packages and updates the Lambda code with `deploy_config.RESERVATION_COMPLETION_WORKER_FUNCTION_NAME`.

When `deploy_config.DATABASE_SECRET_ARN` is present, the worker receives only `DATABASE_SECRET_ARN` and loads the connection string from Secrets Manager at bootstrap. That keeps the full database URL out of the Lambda environment variables while preserving local runs that set `DATABASE_URL` directly.

Terraform also creates two CloudWatch alarms for production visibility:

1. Lambda `Errors` for the reservation completion worker.
2. EventBridge `FailedInvocations` for the reservation completion worker schedule target.

If you want notifications, set `reservation_completion_worker_alarm_actions` to one or more SNS topic ARNs or other CloudWatch alarm action ARNs.

### Reservation completion worker rollback and fix-forward runbook

Use this when the scheduled worker is failing, completing reservations incorrectly, or you need to stop repeated invocations before a safe redeploy.

#### 1. Stop the schedule immediately

Use Terraform outputs so operators do not guess names:

```powershell
$env:AWS_REGION = "us-east-2"
$workerFunction = terraform -chdir=app-backend/infra/root output -raw reservation_completion_worker_function_name
$scheduleRule = terraform -chdir=app-backend/infra/root output -raw reservation_completion_worker_schedule_rule_name

aws events disable-rule --name "$scheduleRule" --region "$env:AWS_REGION"
aws lambda get-function --function-name "$workerFunction" --region "$env:AWS_REGION" --query 'Configuration.{FunctionName:FunctionName,Version:Version,LastModified:LastModified,LastUpdateStatus:LastUpdateStatus}'
```

For a durable stop managed by infrastructure, set `reservation_completion_worker_enabled = false` in the environment tfvars and apply Terraform. That removes the EventBridge schedule instead of relying on a manual console toggle.

#### 2. Roll back to a known-good worker package

Preferred path: redeploy from a known-good Git commit or tag using the existing scripts.

```powershell
git checkout <known-good-commit-or-tag>
bash .github/scripts/package-reservation-completion-worker.sh
$env:RESERVATION_COMPLETION_WORKER_FUNCTION_NAME = terraform -chdir=app-backend/infra/root output -raw reservation_completion_worker_function_name
bash .github/scripts/deploy-reservation-completion-worker.sh
```

This reuses the same package and deploy flow as GitHub Actions and avoids ad-hoc Lambda console edits. After the code update finishes, inspect the latest CloudWatch logs for the worker before re-enabling the schedule.

#### 3. Fix forward safely

Keep the schedule disabled while preparing the fix. Validate the worker before re-enabling periodic execution:

```powershell
cd app-backend/api
npm run lint
npm run test:ci
$env:RUN_PRISMA_RESERVATION_REPOSITORY_DB_TESTS = "true"
$env:PRISMA_RESERVATION_REPOSITORY_DB_TEST_DATABASE_URL = "postgresql://postgres:postgres@localhost:5432/mejengueros_ci"
npm run test:reservation-completion:db
npm run build
```

Then package and deploy with the same scripts:

```powershell
cd ../..
bash .github/scripts/package-reservation-completion-worker.sh
$env:RESERVATION_COMPLETION_WORKER_FUNCTION_NAME = terraform -chdir=app-backend/infra/root output -raw reservation_completion_worker_function_name
bash .github/scripts/deploy-reservation-completion-worker.sh
```

When logs look healthy, re-enable the schedule:

```powershell
$scheduleRule = terraform -chdir=app-backend/infra/root output -raw reservation_completion_worker_schedule_rule_name
aws events enable-rule --name "$scheduleRule" --region "$env:AWS_REGION"
```

If the redeploy still fails, leave the schedule disabled and fix forward again instead of letting the broken worker continue to run.

## Bootstrap Order

1. Apply Terraform with ECR and the static web site enabled.
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
