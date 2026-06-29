import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

export class ComplexNotFoundForOwnerError extends DomainError {
  constructor(complexId: string) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'Complex not found for the authenticated owner.',
      logContext: { complexId }
    });
  }
}
