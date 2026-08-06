import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an administrator targets a user account that does not exist.
 */
export class UserNotFoundError extends DomainError {
  constructor(userId: string) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'User account not found.',
      internalMessage: 'Administrative user account lookup returned no result.',
      logContext: { userId }
    });

    this.name = 'UserNotFoundError';
  }
}
