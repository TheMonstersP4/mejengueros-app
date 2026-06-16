# Prisma Standards

This project uses Prisma ORM 7 style configuration.

References:

- Prisma Config API: https://www.prisma.io/docs/orm/reference/prisma-config-reference
- Prisma ORM 7 upgrade guide: https://www.prisma.io/docs/guides/upgrade-prisma-orm/v7
- Prisma Client generation: https://docs.prisma.io/docs/v6/orm/prisma-client/setup-and-configuration/generating-prisma-client

## Schema

Do not put database URLs in `schema.prisma`.

Use:

```prisma
datasource db {
  provider = "postgresql"
}
```

Do not use:

```prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}
```

Prisma ORM 7 moves connection configuration to `prisma.config.ts`.

## Config

Keep the Prisma CLI configuration in:

```text
api/prisma.config.ts
```

The config owns:

- Schema path.
- Migration path.
- Database URL for Prisma CLI commands.

Use `process.env.DATABASE_URL ?? ''` when `prisma generate` must work without a database URL in CI or local type-checking.

## Client Generator

Use the newer `prisma-client` generator with an explicit output path:

```prisma
generator client {
  provider   = "prisma-client"
  output     = "../src/generated/prisma"
  engineType = "client"
}
```

Generated Prisma Client code is build output and must stay ignored by git:

```text
api/src/generated/prisma/
```

Run `npm run prisma:generate` after changing `schema.prisma`.

## Runtime Client

Use `@prisma/adapter-pg` with the generated client.

Prisma runtime setup belongs in:

```text
api/src/shared/infrastructure/database/prisma.service.ts
```

Feature repositories receive `PrismaService` through Nest DI. Domain and application code must not import the generated Prisma Client or Prisma model types.

## Imports

Allowed in infrastructure:

```ts
import { PrismaClient } from '../../../generated/prisma/client';
import { PrismaPg } from '@prisma/adapter-pg';
```

Allowed in mappers:

```ts
import type { User } from '../../../../generated/prisma/client';
```

Avoid:

```ts
import { PrismaClient } from '@prisma/client';
import type { User } from '@prisma/client';
```

The generated client path makes Prisma 7 output explicit and avoids relying on `node_modules/.prisma`.

## Commands

```bash
npm run prisma:generate
npm run prisma:validate
npm run build
```

`npm run build` runs `prisma:generate` first.

## Pooling

Connection pooling is handled by the `pg` driver. If we need to tune pool behavior, update `PrismaService`, not the datasource URL.

Legacy Prisma URL parameters such as `connection_limit`, `pool_timeout`, and `connect_timeout` should be removed before passing the URL to `pg`.

