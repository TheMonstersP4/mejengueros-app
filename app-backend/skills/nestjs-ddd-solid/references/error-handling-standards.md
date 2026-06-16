# Error Handling Reference

Use typed errors internally and RFC 9457 Problem Details at the HTTP boundary.

Rules:

- Expected errors extend `BaseError`.
- Use `DomainError`, `ApplicationError`, or `InfrastructureError` depending on the layer.
- Shared base errors live in the matching shared layer.
- Feature-specific errors live inside `modules/<feature>/domain/errors` unless the failure belongs to application or infrastructure.
- Interfaces use the `I` prefix.
- Error codes live in `APP_ERROR_CODES`.
- Error codes are uppercase snake case and stable once exposed.
- `userMessage` is safe for API clients.
- `internalMessage`, `logContext`, and `cause` are for logs only.
- Controllers stay thin and do not catch expected application errors.
- The global exception filter converts errors to `application/problem+json`.
- The global exception filter logs with Pino.

Kind to HTTP status:

```text
auth       -> 401
forbidden  -> 403
validation -> 400
not_found  -> 404
conflict   -> 409
external   -> 502
internal   -> 500
```

Current base codes:

```text
BAD_REQUEST              -> 400
AUTH_INVALID_TOKEN       -> 401
AUTH_MISSING_TOKEN       -> 401
FORBIDDEN                -> 403
VALIDATION_FAILED        -> 400
RESOURCE_NOT_FOUND       -> 404
METHOD_NOT_ALLOWED       -> 405
CONFLICT                 -> 409
PAYLOAD_TOO_LARGE        -> 413
UNSUPPORTED_MEDIA_TYPE   -> 415
RATE_LIMITED             -> 429
EXTERNAL_SERVICE_ERROR   -> 502
SERVICE_UNAVAILABLE      -> 503
GATEWAY_TIMEOUT          -> 504
INTERNAL_SERVER_ERROR    -> 500
```

Problem `type` defaults to `urn:problem-type:backend:<error-slug>`. Set
`ERROR_DOCUMENTATION_BASE_URL` to emit documentation URLs instead.

Do not model every HTTP status as a domain error. Keep domain/application codes
business-specific and let the HTTP boundary normalize protocol-level failures.

Do not put NestJS, Fastify, Prisma, AWS SDK, or HTTP status dependencies in domain code.
