import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { ErrorLogLevel } from '../../../../shared/domain/errors/base.error';
import { InfrastructureError } from '../../../../shared/infrastructure/errors/infrastructure.error';

/**
 * Raised when the users list cannot be loaded from persistence.
 */
export class UserListUnavailableError extends InfrastructureError {
  constructor(cause: unknown) {
    super({
      code: APP_ERROR_CODES.SERVICE_UNAVAILABLE,
      kind: 'external',
      userMessage: 'User list is not available right now.',
      internalMessage: 'Failed to load the application users list from persistence.',
      logLevel: ErrorLogLevel.Error,
      httpStatus: 503,
      cause
    });

    this.name = 'UserListUnavailableError';
  }
}
