# DDD And SOLID Reference

Domain contains business rules and does not import framework or infrastructure packages.

Application coordinates use cases and depends on domain and ports.

Infrastructure implements ports with Prisma, Cognito, S3, email, queues, or other adapters.

Interfaces expose HTTP controllers, guards, request DTOs, response DTOs, filters, decorators, and presenters.

Error placement:

- Shared base errors live in `shared/domain/errors`, `shared/application/errors`, and `shared/infrastructure/errors`.
- Feature errors live inside the module that owns the rule, usually under `modules/<feature>/domain/errors`.
- HTTP exception filters live under `shared/interfaces/http/filters`.
- Controllers do not map errors to responses.

SOLID rules:

- One use case per business action.
- Ports stay small.
- TypeScript interfaces use the `I` prefix and keep role suffixes, for example `ITokenVerifierPort` and `IUserRepository`.
- Infrastructure implementations must honor port contracts.
- Add adapters instead of branching inside business code.
- Application depends on abstractions; infrastructure provides implementations.
- Pino is the logger standard; avoid `console.*` in application code.
