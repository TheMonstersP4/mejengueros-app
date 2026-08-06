import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

export class AdminRoleRequiredError extends DomainError {
  constructor(cognitoSub: string) {
    super({
      code: APP_ERROR_CODES.FORBIDDEN,
      kind: 'forbidden',
      userMessage: 'Only administrators can deactivate courts.',
      internalMessage: 'Authenticated user is missing administrator privileges.',
      logContext: { cognitoSub }
    });

    this.name = 'AdminRoleRequiredError';
  }
}
