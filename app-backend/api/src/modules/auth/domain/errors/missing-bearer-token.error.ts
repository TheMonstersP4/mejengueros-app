import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { ErrorLogLevel } from '../../../../shared/domain/errors/base.error';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when a protected HTTP route receives no bearer token.
 */
export class MissingBearerTokenError extends DomainError {
  constructor() {
    super({
      code: APP_ERROR_CODES.AUTH_MISSING_TOKEN,
      kind: 'auth',
      userMessage: 'Authentication token is required.',
      internalMessage: 'Missing Authorization bearer token.',
      logLevel: ErrorLogLevel.Warn
    });

    this.name = 'MissingBearerTokenError';
  }
}
