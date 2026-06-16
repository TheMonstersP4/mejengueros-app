# Error Handling Standards

This project uses typed application errors internally and RFC 9457 Problem Details at the HTTP boundary.

References:

- NestJS exception filters: https://docs.nestjs.com/exception-filters
- RFC 9457 Problem Details for HTTP APIs: https://www.rfc-editor.org/rfc/rfc9457.html

## Goals

- Controllers should not use `try/catch` for normal application errors.
- Domain, application, and infrastructure layers can throw errors that extend `BaseError`.
- HTTP responses should be stable for clients and safe for users.
- Logs should include internal context without leaking stack traces or provider details to the client.

## Internal Error Shape

All expected errors extend `BaseError`.

```ts
export interface IBaseErrorProps {
  code: IAppErrorCode;
  kind: IErrorKind;
  userMessage: string;
  internalMessage?: string;
  logContext?: Record<string, unknown>;
  logLevel?: IErrorLogLevel;
  cause?: unknown;
}
```

Field rules:

- `code`: stable machine-readable code, defined in `APP_ERROR_CODES`.
- `kind`: transport-neutral category used by HTTP filters to choose a status code.
- `userMessage`: safe message returned to API clients.
- `internalMessage`: private message for logs.
- `logContext`: structured diagnostic data for logs.
- `cause`: original provider/SDK error when wrapping infrastructure failures.

## Error Layers

Use the base class that matches the layer:

```text
shared/domain/errors/BaseError
shared/domain/errors/DomainError
shared/application/errors/ApplicationError
shared/infrastructure/errors/InfrastructureError
```

Domain code should not import NestJS, HTTP, Prisma, AWS SDK, or Fastify errors. It should throw domain/application errors and let the boundary translate them.

Feature-specific errors live close to the business rule:

```text
modules/auth/domain/errors/invalid-token.error.ts
modules/users/domain/errors/user-not-found.error.ts
modules/billing/application/errors/payment-declined.error.ts
modules/files/infrastructure/errors/s3-upload-failed.error.ts
```

Shared base classes live in `shared/` because every module can extend them.
The HTTP filter lives in `shared/interfaces/http/filters` because response
formatting is a delivery-layer concern.

## Error Codes

Keep error codes centralized in:

```text
api/src/shared/domain/errors/app-error-code.ts
```

Naming rules:

- Use uppercase snake case.
- Group by business or technical area.
- Do not reuse a code with a different meaning.
- Do not expose provider-specific codes directly to clients.

Examples:

```text
AUTH_INVALID_TOKEN
AUTH_MISSING_TOKEN
VALIDATION_FAILED
RESOURCE_NOT_FOUND
EXTERNAL_SERVICE_ERROR
INTERNAL_SERVER_ERROR
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

Most expected errors use `kind`, and the HTTP filter maps that kind at the
boundary. Use `httpStatus` only when one error needs a more specific status than
its kind, such as `PAYLOAD_TOO_LARGE` or `UNSUPPORTED_MEDIA_TYPE`.

Do not create one domain error for every HTTP status. Add application-specific
codes when product behavior needs them. HTTP-only cases such as 405, 413, 415,
429, 503, and 504 are usually produced by the HTTP framework, API Gateway, rate
limiters, or infrastructure adapters.

## HTTP Response

HTTP errors are serialized by `ApiExceptionFilter` using the same envelope as
successful responses.

Response shape:

```json
{
  "success": false,
  "data": null,
  "errors": [
    {
      "code": "AUTH_INVALID_TOKEN",
      "message": "Authentication token is invalid or expired.",
      "status": 401,
      "type": "urn:problem-type:backend:auth-invalid-token"
    }
  ],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/auth/me",
    "timestamp": "2026-06-05T00:00:00.000Z"
  }
}
```

`errors[].type` keeps the problem type identifier. Clients can use it to detect
the error category, and humans can use it as a documentation link when
`ERROR_DOCUMENTATION_BASE_URL` is configured.

Default behavior:

```text
ERROR_DOCUMENTATION_BASE_URL unset -> urn:problem-type:backend:<error-slug>
ERROR_DOCUMENTATION_BASE_URL set   -> <base-url>/<error-slug>
```

Do not include stack traces, SQL details, AWS request payloads, OAuth tokens, cookies, or raw provider responses in HTTP responses.

## Status Mapping

The filter maps `kind` to HTTP status:

```text
auth       -> 401
forbidden  -> 403
validation -> 400
not_found  -> 404
conflict   -> 409
external   -> 502
internal   -> 500
```

If a case needs a different status, create a clear error kind or an HTTP-specific adapter at the boundary. Do not put HTTP status codes inside domain errors.

## Logging

Log internal details, not user-facing details.

Recommended fields:

```text
method
url
status
code
traceId
internalMessage
context
stack
```

Use `warn` for expected 4xx application errors and `error` for unexpected failures or 5xx responses.

The NestJS API logs through Pino. Do not use `console.*` in application code.
The global Problem Details filter logs through `PinoLogger`.

## Creating A New Error

```ts
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class UserNotFoundError extends DomainError {
  constructor(userId: string) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'User was not found.',
      internalMessage: 'User lookup returned no result.',
      logContext: { userId }
    });

    this.name = 'UserNotFoundError';
  }
}
```

## Controller Rule

Controllers should stay thin:

```ts
@Get(':id')
async findById(@Param('id') id: string): Promise<UserResponse> {
  return this.findUserById.execute({ id });
}
```

The use case or adapter throws the error. The global filter formats the response.

## Validation Errors

Request validation stays at the HTTP boundary. Validation pipe errors are converted by the global filter to `VALIDATION_FAILED`.

Domain validation still belongs in value objects and domain services. Example: an `Email` value object can throw `InvalidEmailError` even if the HTTP DTO already validated the input.
