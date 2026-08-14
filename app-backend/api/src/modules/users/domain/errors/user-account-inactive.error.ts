import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an inactive local account tries to use protected user flows.
 */
export class UserAccountInactiveError extends DomainError {
  constructor(cognitoSub: string, userId: string) {
    super({
      code: APP_ERROR_CODES.FORBIDDEN,
      kind: 'forbidden',
      userMessage: 'User account is inactive.',
      internalMessage: 'Inactive local user account attempted an operational flow.',
      logContext: { cognitoSub, userId }
    });

    this.name = 'UserAccountInactiveError';
  }
}
