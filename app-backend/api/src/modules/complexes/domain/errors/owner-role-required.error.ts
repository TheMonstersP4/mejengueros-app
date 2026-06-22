import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an authenticated user tries to create a complex without OWNER role.
 */
export class OwnerRoleRequiredError extends DomainError {
  constructor(cognitoSub: string) {
    super({
      code: APP_ERROR_CODES.FORBIDDEN,
      kind: 'forbidden',
      userMessage: 'Only users with the OWNER role can create complexes.',
      internalMessage: 'Authenticated user is missing OWNER role for complex creation.',
      logContext: { cognitoSub }
    });

    this.name = 'OwnerRoleRequiredError';
  }
}
