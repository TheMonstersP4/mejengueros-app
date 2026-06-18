# Local Prisma migration validation Docker environment

This folder exists only to validate Prisma migrations locally against a disposable PostgreSQL instance.

## Quick path

1. Copy `docker/migration-validation.env.example` to `docker/migration-validation.env.local`.
2. Replace the placeholder values in `docker/migration-validation.env.local` with disposable local-only credentials.
3. Start PostgreSQL: `npm run docker:migration-db:up`
4. Load `DATABASE_URL` from your local env file, then run Prisma validation and migrations from `app-backend/api`.

## Purpose

- Validate Prisma schema generation and migration execution for issue `#29`.
- Keep the environment local, disposable, and isolated from shared or production databases.

## Files

| File | Purpose |
|---|---|
| `docker-compose.migration-validation.yml` | Starts one local PostgreSQL service for migration checks. |
| `migration-validation.env.example` | Versioned template with placeholders only. |
| `migration-validation.env.local` | Ignored local env file used by Docker Compose scripts and Prisma commands. |

## Commands

### Start

```powershell
npm run docker:migration-db:up
```

If the local env file does not exist yet:

```powershell
Copy-Item .\docker\migration-validation.env.example .\docker\migration-validation.env.local
```

### Stop

```powershell
npm run docker:migration-db:down
```

### Reset everything, including data

```powershell
npm run docker:migration-db:reset
```

## Local env file contract

`docker/migration-validation.env.local` should define:

```dotenv
MIGRATION_DB_NAME=mejengueros_migration_validation
MIGRATION_DB_USER=<local-user>
MIGRATION_DB_PASSWORD=<local-password>
MIGRATION_DB_PORT=54329
DATABASE_URL=postgresql://<local-user>:<local-password>@localhost:54329/mejengueros_migration_validation?schema=mejengueros_dev
```

## Prisma commands

From `app-backend/api`:

```powershell
Get-Content .\docker\migration-validation.env.local |
  Where-Object { $_ -and -not $_.StartsWith('#') } |
  ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value.Trim('"'), 'Process')
  }

npm run prisma:validate
npm run prisma:generate
npx prisma migrate deploy
```

Optional status check after migration:

```powershell
Get-Content .\docker\migration-validation.env.local |
  Where-Object { $_ -and -not $_.StartsWith('#') } |
  ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value.Trim('"'), 'Process')
  }

npx prisma migrate status
```

## Warnings

- Do not use this setup for production.
- Do not point it at shared, long-lived, or team-managed data.
- Keep `docker/migration-validation.env.local` uncommitted.
- `docker:migration-db:reset` deletes the local validation volume on purpose.
