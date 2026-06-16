---
name: unit-testing
description: Write, review, and configure unit tests for the NestJS TypeScript API using Jest, DDD boundaries, SOLID, mocks, coverage, and maintainable test structure. Use when Codex adds tests, audits coverage, configures Jest, improves testability, or enforces unit testing standards.
---

# Unit Testing

Use this skill for unit test implementation and Jest coverage work in the API.

## Workflow

1. Inspect the target code before writing tests.
2. Identify the unit boundary: pure function, entity, mapper, use case, controller, guard, adapter, or handler.
3. Mock only external collaborators: database, AWS SDK, Cognito verifier, logger, HTTP framework, clock, or random values.
4. Test observable behavior, not private implementation.
5. Run focused tests first, then full coverage.
6. Do not weaken production code to make tests pass.

## Test Shape

Use Arrange, Act, Assert with clear names:

```ts
describe('SyncAuthenticatedUserUseCase', () => {
  it('creates a user profile when the authenticated subject is new', async () => {
    // arrange
    // act
    // assert
  });
});
```

Rules:

- Prefer one behavior per test.
- Use realistic values, not `foo`/`bar`, when domain meaning matters.
- Keep mocks local to the test unless shared setup reduces noise.
- Reset mocks between tests.
- Assert calls only when the call is part of the contract.
- Cover happy path, expected errors, edge cases, and provider failures.

## DDD Boundaries

Domain:

- Test without NestJS testing module.
- Do not mock the entity under test.
- Assert invariants and domain errors.

Application:

- Mock ports and repositories.
- Assert orchestration and returned DTOs.
- Do not import Prisma or AWS SDK.

Infrastructure:

- Mock external SDK clients and Prisma.
- Assert translation between provider/persistence models and application contracts.
- Wrap provider failures in typed infrastructure errors.

Interfaces:

- Controllers should be tested with mocked use cases.
- Guards should mock token verifiers and request objects.
- Filters should assert HTTP response shape and logging behavior.

## Coverage

Coverage must include real source files and exclude generated or structural files:

```text
src/generated/**
src/**/index.ts
src/**/*.spec.ts
```

Use `npm run test:cov` from `api/`. The terminal output must show the coverage table with file percentages. Keep `lcov` enabled for CI or HTML tooling.

Do not chase 100% by testing implementation trivia. If a file cannot be meaningfully unit tested, prefer extracting logic into a testable unit instead of excluding it silently.

## References

Read when deeper guidance is needed:

- `references/unit-test-standards.md`
