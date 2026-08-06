import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an administrator requests a role outside the user role catalog.
 */
export class InvalidUserRoleError extends DomainError {
  constructor(role: string) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: 'User role is not valid.',
      internalMessage: 'Administrative user account update received an unsupported role.',
      logContext: { role }
    });

    this.name = 'InvalidUserRoleError';
  }
}
