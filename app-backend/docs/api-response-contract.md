# API Response Contract

All JSON HTTP responses use the same envelope:

```json
{
  "success": true,
  "data": {},
  "errors": [],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/health",
    "timestamp": "2026-06-05T00:00:00.000Z"
  }
}
```

## Successful Responses

Controllers return endpoint data directly. The global response interceptor wraps
that data into the standard envelope.

Example:

```json
{
  "success": true,
  "data": {
    "id": "user_123",
    "email": "user@example.com"
  },
  "errors": [],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/users/me",
    "timestamp": "2026-06-05T00:00:00.000Z"
  }
}
```

Endpoints that need pagination or additional response metadata can use
`withApiMeta(data, meta)` from the shared HTTP response helpers.

## Error Responses

Expected domain, application, and infrastructure errors extend `BaseError`.
Controllers should not catch those errors just to convert them to HTTP errors.
The global exception filter maps them to the API envelope.

Example:

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

HTTP status codes remain meaningful. A failed request still returns `4xx` or
`5xx`; the envelope only standardizes the JSON shape.

## Layer Rules

- `domain` raises business policy errors.
- `application` orchestrates use cases and lets expected errors bubble up.
- `infrastructure` wraps provider failures in infrastructure errors.
- `interfaces/http` validates DTOs and returns controller data only.
- Global HTTP filter and interceptor own the response envelope.
