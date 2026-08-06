import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an authenticated user tries to access an admin-only users query.
 */
export class AdminRoleRequiredError extends DomainError {
  constructor(cognitoSub: string) {
    super({
      code: APP_ERROR_CODES.FORBIDDEN,
      kind: 'forbidden',
      userMessage: 'Only administrators can list application users.',
      internalMessage: 'Authenticated user is missing ADMIN role for users list access.',
      logContext: { cognitoSub }
    });

    this.name = 'AdminRoleRequiredError';
  }
}
