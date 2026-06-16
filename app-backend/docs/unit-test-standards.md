# Unit Test Standards

## Naming

Use behavior-focused test names:

```text
returns the authenticated profile for the current user
throws MissingBearerTokenError when the header is absent
stores the WebSocket connection with a TTL
```

Avoid names that repeat implementation details:

```text
calls method x
sets variable y
```

## Mocks

Mock at boundaries only:

- Repository ports
- Token verifier ports
- Prisma client delegates
- AWS SDK clients
- Logger adapters
- HTTP request and response objects

Do not mock:

- Domain entities under test
- Value objects under test
- Mappers under test
- Simple DTOs or constants

## Error Tests

Expected errors must assert:

- Error class
- Stable error code
- Safe user-facing message when exposed
- Internal message or cause only when it belongs to the contract

## Jest Coverage

The API Jest config should:

- Use `ts-jest`
- Map `@/*` to `src/*`
- Exclude `src/generated/**`
- Exclude barrels such as `src/**/index.ts`
- Exclude `src/**/*.spec.ts`
- Print `text` and `text-summary` reporters in the terminal
- Keep `lcov` for CI and tooling

## Coverage Thresholds

Start with thresholds that protect quality without forcing noisy tests:

```text
statements: 90
branches:   85
functions:  90
lines:      90
```

Raise them only when the suite has meaningful coverage for new behavior.

## 100 Percent Coverage

Use 100% coverage as design pressure, not as a vanity number.

Allowed ways to reach it:

- Add meaningful branch tests
- Extract side-effect-free functions
- Move framework glue behind small adapters
- Mock external dependencies at ports

Avoid:

- Testing private methods directly
- Adding exports only for tests
- Adding broad ignore comments
- Excluding files without a clear reason
