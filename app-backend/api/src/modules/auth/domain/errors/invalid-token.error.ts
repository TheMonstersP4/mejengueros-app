import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { ErrorLogLevel } from '../../../../shared/domain/errors/base.error';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when Cognito token verification fails.
 */
export class InvalidTokenError extends DomainError {
  /**
   * Creates an invalid token error.
   *
   * @param cause - Original verifier error kept for internal diagnostics.
   */
  constructor(cause?: unknown) {
    super({
      code: APP_ERROR_CODES.AUTH_INVALID_TOKEN,
      kind: 'auth',
      userMessage: 'Authentication token is invalid or expired.',
      internalMessage: 'Cognito token verification failed.',
      logLevel: ErrorLogLevel.Warn,
      cause
    });

    this.name = 'InvalidTokenError';
  }
}
