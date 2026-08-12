# Mejengueros API

Main backend API for Mejengueros. It is a NestJS API with Fastify, Pino, Cognito, Prisma, and Lambda handlers for API Gateway WebSocket.

The HTTP API exposes versioned endpoints under `/v1`. Cognito owns social login with Google and Microsoft; this API validates tokens issued by Cognito. WebSocket handlers live in the same package, but they are deployed as small Lambdas so each WebSocket event does not need to bootstrap the full HTTP app.

All JSON responses use the standard `success`, `data`, `errors`, and `meta` envelope documented in `docs/api-response-contract.md`.

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
| `DEMO_OWNER_SUBS` | No | Comma-separated Cognito subject allowlist that grants the `OWNER` role during authenticated user reconciliation in demo/MVP environments, including `POST /v1/complexes` and `GET /v1/users/me`. |
| `DEMO_OWNER_EMAILS` | No | Optional fallback comma-separated email allowlist that grants the `OWNER` role only when Cognito also reports `email_verified=true`. |
| `WEBSOCKET_CONNECTIONS_TABLE_NAME` | Yes | DynamoDB table for WebSocket connections. |
| `WEBSOCKET_CONNECTION_TTL_SECONDS` | No | TTL for stale WebSocket connections. Default: `86400`. |
| `WEBSOCKET_ENDPOINT` | No | API Gateway WebSocket URL used by local workers to publish realtime notifications. |

Prisma-backed endpoints are disabled until `DATABASE_URL` is available directly or through `DATABASE_SECRET_ARN`.

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
WEBSOCKET_ENDPOINT=wss://dilk66l4f1.execute-api.us-east-2.amazonaws.com/dev
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

Application tables live in the PostgreSQL schema `mejengueros_dev`. Do not use the shared database `public` schema for this project.

Useful commands:

```powershell
npm run prisma:generate
npm run prisma:validate
npx prisma migrate deploy
```

The generated client lives in `src/generated/prisma` and must not be imported from domain or application code.

### Location and service catalogs for the complex wizard

- `Province` and `Canton` are controlled catalogs for Costa Rica.
- `Canton` belongs to exactly one `Province`.
- `Complex.address` remains the user-visible address/reference string.
- `Complex.latitude` and `Complex.longitude` store the optional map pin coordinates.
- `ServiceCatalog` stays the single source of truth for both complex services and court services, including MVP grass types.
- The migration enforces that a persisted `Complex.cantonId` must belong to the same `Complex.provinceId`.

Current transition rule:

- `provinceId`, `cantonId`, `latitude`, and `longitude` are nullable in the schema for now so the existing `POST /v1/complexes` contract can remain unchanged until the follow-up API issue expands the request body.

## Demo Seed

A minimal seed that populates the database with demo data for the MVP flow: catalogue, detail, availability, and reservation.

### Load or reset

Requires `ALLOW_DEMO_SEED=true`. Does not run against `NODE_ENV=production`.

```powershell
$env:ALLOW_DEMO_SEED="true"; npm run db:seed
```

The seed is idempotent: it tears down existing demo data and inserts a fresh set on each run.

### What it creates

| Entity | Value |
| --- | --- |
| Owner user | `demo-owner@mejengueros.demo` — provider `demo`, subject `demo-owner-sub-00000001`, role `OWNER` |
| Player 1 | `demo-player1@mejengueros.demo` — provider `demo`, subject `demo-player-sub-00000001`, role `PLAYER` |
| Player 2 | `demo-player2@mejengueros.demo` — provider `demo`, subject `demo-player-sub-00000002`, role `PLAYER` |
| Province | San Jose (code `SJ`) |
| Canton | San Jose (code `SJ-01`) |
| Complex | "Complejo Demo Los Nogales" — Av. Central 1234, San Jose, Costa Rica |
| Complex services | Parqueo |
| Court | "Cancha 1 — Demo" |
| Court services | Iluminacion, Sintetico, Natural, Hibrido |
| Availability | Monday to Saturday, 08:00 to 22:00 UTC |
| Confirmed reservation | Next Saturday at 10:00–11:00 UTC — held by Player 1 |
| Completed reservation | 7 days ago at 10:00–11:00 UTC — held by Player 2, includes a 5-star review |

### Demo scenarios

- **Catalogue and detail**: any user can browse the complex and court data from the database.
- **Available slot**: any slot within the Mon–Sat 08:00–22:00 window other than Saturday 10:00 UTC is open to book.
- **Double-booking error**: attempting to book the Saturday 10:00 UTC slot triggers the unique partial index on confirmed reservations and returns the business error.

### Teardown

Identifies demo data by `UserIdentity.provider = 'demo'` or the exact demo emails (`demo-owner@mejengueros.demo`, `demo-player1@mejengueros.demo`, `demo-player2@mejengueros.demo`) and deletes in FK-safe order: reviews, notifications, reservations tied to demo users or demo courts, courts, complexes, and users. Catalog tables (`Province`, `Canton`, `ServiceCatalog`) are shared data and are not removed.

### Local idempotency validation

Use the disposable database in `app-backend/api/docker/`:

```powershell
npm run docker:migration-db:up
# Set DATABASE_URL to the local URL (see docker/migration-validation.env.example)
$env:ALLOW_DEMO_SEED="true"; npm run db:seed
$env:ALLOW_DEMO_SEED="true"; npm run db:seed
npm run docker:migration-db:reset
```

Both runs must complete without errors and leave the expected demo dataset.

## Purge A User

`npm run dev:users:purge` hard-deletes one user and every entity that hangs off them. Unlike the demo seed teardown above, it targets an arbitrary email instead of the `demo` provider, so it works on real dev accounts.

It runs as a **dry run by default**: the whole deletion executes inside a transaction that is rolled back, and the row counts are printed. Nothing is committed unless `--execute=true` is passed.

```bash
# 1. Preview (rolls back, prints what would be deleted)
npm run dev:users:purge -- --email=someone@example.com

# 2. Commit
npm run dev:users:purge -- --email=someone@example.com --execute=true
```

`DATABASE_URL` must point at the target database. For the shared dev database it lives in `app-backend/.env` (not `app-backend/api/.env`, where it is empty):

```bash
export DATABASE_URL="$(rg -N '^DATABASE_URL=(.+)$' -r '$1' ../.env)"
```

### Owned complexes

`Complex.owner` is `onDelete: Restrict`, so a user who owns a complex cannot be deleted while that ownership stands. Owned complexes and their courts are therefore always part of the purge — re-running the script always leaves the account empty, with no second flag to remember.

The one case that stops the run is collateral damage. Other users' reservations and notifications on those courts are `Restrict` as well, so they die in the same pass and cannot be preserved. When any exist the script refuses and reports how many; `--include-owned-complexes=true` accepts the loss. Reassigning `Complex.ownerId` to another user first is the alternative that keeps their history.

When nothing belongs to anybody else there is nothing to confirm, so no flag is needed. The report always carries `scope.collateralReservations` and `scope.collateralNotifications` — read them before passing `--execute=true`.

### Deletion order

Driven by the foreign keys, not by preference: `ReviewQuestionnaireAnswer` → `Review` → `Notification` → `Reservation` → `CourtAvailabilityDay` → `CourtAvailability` → `CourtService` → `Court` → `ComplexService` → `Complex` → `UserRole` → `UserIdentity` → `User` → `ImageUpload`.

Two details worth keeping:

- Notifications are matched by `reservationId OR userId`. A reservation on the purged user's court can carry a notification owned by a different user; matching on `userId` alone leaves it behind and the `Reservation` delete then fails.
- `ImageUpload` goes last because `Court.imageUpload`, `Review.evidenceImageUpload` and `User.profileImageUpload` are `SetNull` — the referencing rows have to be gone first.

### What it does not do

- **S3 objects are not deleted.** The `ImageUpload` rows are removed, the blobs under `dev/uploads/<purpose>/<ownerSub>/` are not.
- **The Cognito user is not deleted.** Identity lives in the user pool, and `sync-authenticated-user.use-case.ts` re-provisions a fresh `User` row on the next login with the same provider subject. Use this script to reset an account's data, not to unregister it. To also unregister, run `aws cognito-idp admin-delete-user --region <region> --user-pool-id <pool> --username <providerSubject>`.

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
