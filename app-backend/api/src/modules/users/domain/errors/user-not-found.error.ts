import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an administrative user operation targets an unknown local user.
 */
export class UserNotFoundError extends DomainError {
  constructor(userId: string) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'User account was not found.',
      internalMessage: 'Local user account was not found for administrative operation.',
      logContext: { userId }
    });

    this.name = 'UserNotFoundError';
  }
}
