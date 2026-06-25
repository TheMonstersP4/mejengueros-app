# Mejengueros Backend

Mejengueros backend and infrastructure.

This subproject contains the API, cloud infrastructure, and a support web POC for social authentication and WebSocket testing.

## What it includes

- `api/`: NestJS API with Fastify, Prisma, Cognito, and Lambda handlers for WebSocket.
- `infra/`: Terraform for AWS, Azure AD, Cloudflare, and environment composition.
- `poc/`: static web client to validate Hosted UI login and the chat flow.
- `docs/`: backend technical standards and architecture documentation.

## Structure

```text
.
|-- api/      NestJS API, Prisma, tests, and WebSocket Lambda handlers
|-- infra/    Terraform by modules and environment composition
|-- poc/      Static web client to test Cognito login + WebSocket
|-- docs/     Technical standards and architecture decisions
`-- ../.github/  Workflows and deploy scripts at repository root
```

## Prerequisites

- Node.js 22 and npm for `api/`.
- Terraform for `infra/`.
- Real Cognito and AWS values when testing protected routes or deployments.

## API

The API is in `api/`.

It includes:

- NestJS with Fastify.
- Pino for HTTP logs.
- Cognito as the identity broker for Google and Microsoft.
- Prisma 7 for persistence.
- DDD structure by module.
- Unit tests outside `src`.
- Lambda handlers for API Gateway WebSocket.

Local guide:

```text
api/README.md
```

Main commands:

```powershell
cd api
npm install
npm run start:dev
npm run lint
npm test -- --runInBand
npm run test:cov -- --runInBand
npm run build
```

The local API exposes routes under:

```text
http://localhost:3000/v1
```

## Infrastructure

Terraform is in `infra/`.

It includes:

- Cognito User Pool with Google and Microsoft as identity providers.
- Microsoft Entra app registration that can be automated with Terraform.
- Private VPC without a NAT gateway by default.
- S3 for application files.
- ECR for Docker images.
- API Gateway WebSocket with DynamoDB for room connections.
- Lambdas for the `$connect`, `$disconnect`, and `$default` routes.
- CloudWatch log groups with defined retention.
- POC site in S3 + Cloudflare Worker.
- GitHub Actions OIDC deploy role.

Structure:

```text
infra/
|-- env/      tfvars and backend examples by environment
|-- modules/  reusable modules
`-- root/     environment composition
```

Before running Terraform, create your real local files from the versioned examples:

```powershell
Copy-Item 'infra\env\dev.tfvars.example' 'infra\env\dev.tfvars'
Copy-Item 'infra\env\dev.backend.hcl.example' 'infra\env\dev.backend.hcl'
```

Commands from `app-backend/`:

```powershell
terraform -chdir=infra/root init
terraform -chdir=infra/root plan -var-file '..\env\dev.tfvars'
terraform -chdir=infra/root apply -var-file '..\env\dev.tfvars'
```

If you use a remote backend:

```powershell
terraform -chdir=infra/root init -backend-config '..\env\dev.backend.hcl'
```

## Web POC

The POC is in `poc/web-chat`.

It is used to test:

- Login with Cognito Hosted UI.
- OAuth callback with PKCE.
- Protected `/chat/` page.
- WebSocket connection.
- Messages and connected users by room.

Local:

```powershell
cd poc/web-chat
python -m http.server 3000
```

Open:

```text
http://localhost:3000
```

## Documentation

`docs/` is the canonical source for project standards.

Main documents:

- `docs/nestjs-ddd-structure.md`
- `docs/ddd-solid-standards.md`
- `docs/error-handling-standards.md`
- `docs/prisma-standards.md`
- `docs/terraform-standards.md`
- `docs/tsdoc-standards.md`
- `docs/unit-test-standards.md`
- `docs/websocket-architecture.md`

## Where to go deeper

- [`api/README.md`](api/README.md): environment variables, endpoints, quality, and API architecture.
- [`infra/README.md`](infra/README.md): Terraform modules, inputs/outputs, and infrastructure deployment.
- [`poc/web-chat/README.md`](poc/web-chat/README.md): web POC flow for authentication and chat.
- [`docs/`](docs/): backend standards and technical decisions.

## Deploy

GitHub Actions lives in the repository root `.github/` folder, not inside `app-backend/`.

Deployment is script-driven:

- The workflow detects changes.
- It uses OIDC to assume the AWS role.
- It runs the quality gate before publishing the API and WebSocket.
- The scripts live at the repo-root path `.github/scripts`.

Required secrets for the `dev` environment:

```text
AWS_ROLE_ARN_DEV
DEPLOY_DEV_CONFIG
```

They are obtained from Terraform:

```powershell
terraform -chdir=infra/root output -raw github_actions_iam_role_arn
terraform -chdir=infra/root output -json deploy_config
```

## Security

Do not commit:

- `.env`
- real `.tfvars`
- real backend configs
- OAuth secrets
- database passwords
- Terraform state
- generated Lambda packages
- Prisma generated client
- coverage or build output

Examples live as `*.example`, and real values are handled locally or through environment secrets.
