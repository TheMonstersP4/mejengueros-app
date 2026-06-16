# Prisma Reference

Use Prisma ORM 7 style configuration.

Rules:

- `schema.prisma` defines the datasource provider only.
- Database URLs live in `prisma.config.ts`, not in `schema.prisma`.
- Use `generator client { provider = "prisma-client"; output = "../src/generated/prisma"; engineType = "client" }`.
- Generated client output is ignored by git.
- Runtime code creates Prisma Client with `@prisma/adapter-pg`.
- Domain and application code must not import Prisma Client or Prisma model types.
- Feature repositories use `PrismaService`; mappers may import generated model types only as `type`.
- Run `npm run prisma:generate` after schema changes.

Allowed infrastructure imports:

```ts
import { PrismaClient } from '../../../generated/prisma/client';
import { PrismaPg } from '@prisma/adapter-pg';
```

Avoid:

```ts
import { PrismaClient } from '@prisma/client';
```

