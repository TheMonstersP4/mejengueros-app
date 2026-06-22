# Mejengueros API

Main backend API for Mejengueros. It is a NestJS API with Fastify, Pino, Cognito, Prisma, and Lambda handlers for API Gateway WebSocket.

The HTTP API exposes versioned endpoints under `/v1`. Cognito owns social login with Google and Microsoft; this API validates tokens issued by Cognito. WebSocket handlers live in the same package, but they are deployed as small Lambdas so each WebSocket event does not need to bootstrap the full HTTP app.

All JSON responses use the standard `success`, `data`, `errors`, and `meta`
envelope documented in `docs/api-response-contract.md`.

## Requirements

- Node.js 22
- npm
- Real Cognito values when testing protected routes

## Local Environment

Copy the example and adjust values:

```powershell
Copy-Item .env.example .env
```

Variables validated when the API starts:

| Variable | Required | Usage |
| --- | --- | --- |
| `NODE_ENV` | No | Runtime environment: `development`, `test`, or `production`. |
| `PORT` | No | Local HTTP port. Default: `3000`. |
| `LOG_LEVEL` | No | Pino level: `trace`, `debug`, `info`, `warn`, `error`, or `fatal`. |
| `ERROR_DOCUMENTATION_BASE_URL` | No | Base URL for error documentation links. Can stay empty locally. |
| `APP_CORS_ALLOWED_ORIGINS` | No | Comma-separated browser origins allowed to call the API. |
| `DATABASE_URL` | No | PostgreSQL URL used by Prisma. Include `schema=mejengueros_dev` when using the shared Azure database. |
| `DATABASE_SECRET_ARN` | No | AWS Secrets Manager ARN used by Lambda to load `DATABASE_URL` at startup. |
| `AWS_REGION` | Yes | AWS region where Cognito is deployed. |
| `APP_S3_BUCKET_NAME` | Yes | Private application S3 bucket used for image uploads. |
| `APP_S3_REGION` | No | S3 bucket region. Defaults to `AWS_REGION`. |
| `APP_S3_KEY_PREFIX` | No | Prefix used for generated S3 object keys. Default: `uploads`. |
| `APP_S3_UPLOAD_URL_TTL_SECONDS` | No | Time-to-live for presigned upload forms. Default: `300`. |
| `APP_S3_PROFILE_IMAGE_MAX_BYTES` | No | Maximum profile image size. Default: `5242880`. |
| `APP_S3_ALLOWED_IMAGE_MIME_TYPES` | No | Comma-separated allowed image MIME types. |
| `COGNITO_USER_POOL_ID` | Yes | Cognito User Pool ID. |
| `COGNITO_CLIENT_ID` | Yes | Cognito App Client ID. |
| `COGNITO_TOKEN_USE` | No | Token type accepted by the API: `id` or `access`. Default: `id`. |
| `DEMO_OWNER_SUBS` | No | Preferred comma-separated Cognito subject allowlist that grants the local `OWNER` role during authenticated user reconciliation in demo/MVP environments, including `POST /v1/complexes` and `GET /v1/users/me`. |
| `DEMO_OWNER_EMAILS` | No | Optional fallback comma-separated email allowlist that grants the local `OWNER` role only when Cognito also reports `email_verified=true`. |
| `WEBSOCKET_CONNECTIONS_TABLE_NAME` | Yes | DynamoDB table for WebSocket connections. |
| `WEBSOCKET_CONNECTION_TTL_SECONDS` | No | TTL for stale WebSocket connections. Default: `86400`. |

Prisma-backed endpoints are disabled until `DATABASE_URL` is available directly
or through `DATABASE_SECRET_ARN`.

Minimal local example:

```env
NODE_ENV=development
PORT=3000
LOG_LEVEL=debug
ERROR_DOCUMENTATION_BASE_URL=
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
DATABASE_URL=
DATABASE_SECRET_ARN=

AWS_REGION=us-east-2
APP_S3_BUCKET_NAME=mejengueros-dev-app-example
APP_S3_REGION=us-east-2
APP_S3_KEY_PREFIX=dev/uploads
APP_S3_UPLOAD_URL_TTL_SECONDS=300
APP_S3_PROFILE_IMAGE_MAX_BYTES=5242880
APP_S3_ALLOWED_IMAGE_MIME_TYPES=image/jpeg,image/png,image/webp

COGNITO_USER_POOL_ID=us-east-2_example
COGNITO_CLIENT_ID=example-client-id
COGNITO_TOKEN_USE=id
DEMO_OWNER_SUBS=owner-sub-from-cognito
DEMO_OWNER_EMAILS=

WEBSOCKET_CONNECTIONS_TABLE_NAME=mejengueros-dev-ws-connections
WEBSOCKET_CONNECTION_TTL_SECONDS=86400
```

## Run Locally

Install dependencies:

```powershell
npm install
```

Generate Prisma:

```powershell
npm run prisma:generate
```

Start the API:

```powershell
npm run start:dev
```

The API is available at:

```text
http://localhost:3000/v1
```

## Test The API Locally

Health check:

```powershell
curl http://localhost:3000/v1/health
```

Expected response:

```json
{
  "status": "ok",
  "timestamp": "2026-05-25T00:00:00.000Z"
}
```

Protected routes:

```powershell
curl http://localhost:3000/v1/auth/me -H "Authorization: Bearer <id_token>"
curl http://localhost:3000/v1/users/me -H "Authorization: Bearer <id_token>"
curl -X POST http://localhost:3000/v1/complexes -H "Authorization: Bearer <id_token>" -H "Content-Type: application/json" -d '{"complex":{"name":"North Sports Center","address":"123 Main Street"},"firstCourt":{"name":"Court A"}}'
```

The `<id_token>` must come from Cognito Hosted UI. Do not send raw Google or Microsoft tokens directly to this API.

## Current Endpoints

| Method | Route | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/v1/health` | No | Basic process health. |
| `GET` | `/v1/auth/me` | Yes | Returns the authenticated user from the Cognito token. |
| `POST` | `/v1/files/uploads` | Yes | Creates a presigned S3 POST form for profile images. |
| `POST` | `/v1/files/uploads/confirm` | Yes | Confirms a direct S3 image upload and validates ownership, metadata, and byte signature. |
| `GET` | `/v1/users/me` | Yes | Syncs and returns the local profile for the authenticated user. Loaded once PostgreSQL is enabled. |
| `POST` | `/v1/complexes` | Yes | Creates a complex and its first court for authenticated users whose OWNER access is reconciled inside the request itself. |

> Limitation: the current `UserRole` schema does not store a role source. Demo OWNER reconciliation is therefore grant-only: it can add `OWNER` for allowlisted identities, but it does not revoke existing `OWNER` rows. Source metadata is required before demo-managed roles can be safely revoked independently from manual or future admin-managed assignments.

## WebSocket Lambdas

Handlers live in:

```text
src/functions/websocket
```

Routes:

```text
$connect    -> functions/websocket/connect.handler
$disconnect -> functions/websocket/disconnect.handler
$default    -> functions/websocket/default.handler
```

Generate the zip consumed by Terraform and GitHub Actions:

```powershell
npm run build
npm run lambda:package:websocket
```

The package is written to:

```text
api/.lambda/websocket.zip
```

## Prisma

The project uses Prisma 7:

```text
prisma/schema.prisma
prisma.config.ts
src/generated/prisma
```

Application tables live in the PostgreSQL schema `mejengueros_dev`. Do not use
the shared database `public` schema for this project.

Useful commands:

```powershell
npm run prisma:generate
npm run prisma:validate
npx prisma migrate deploy
```

The generated client lives in `src/generated/prisma` and must not be imported from domain or application code.

## Quality

Main commands:

```powershell
npm run lint
npm test -- --runInBand
npm run test:integration -- --runInBand
npm run test:cov -- --runInBand
npm run build
```

Unit tests live in `test/unit`. Do not place `*.spec.ts` files inside `src`.

## Architecture

The API follows modular DDD:

```text
src/modules/<feature>/
  domain/
  application/
  infrastructure/
  interfaces/
```

Quick rules:

- Keep controllers thin.
- Put use cases in `application`.
- Keep entities, value objects, and business errors in `domain`.
- Keep Prisma, Cognito, AWS SDK, and external adapters in `infrastructure`.
- Keep HTTP controllers, guards, filters, and DTOs in `interfaces`.
- Use `shared` only for stable technical primitives.
