# Unit Test Standards Checklist

Canonical source: `docs/unit-test-standards.md`.

Use this file as the quick checklist when writing or reviewing tests.

## Checklist

- Test behavior, not private implementation.
- Mock only real boundaries: repositories, SDKs, Prisma, token verifiers, logger adapters, framework requests, clocks, and random values.
- Domain tests do not import NestJS, Prisma, AWS SDK, or HTTP types.
- Application tests mock ports and assert orchestration.
- Infrastructure tests assert provider translation and typed error wrapping.
- Interface tests use mocked use cases, guards, filters, and request objects.
- Expected errors assert class, stable code, and safe message.
- Coverage excludes generated code and barrels.
- Coverage must print file percentages in the terminal.
