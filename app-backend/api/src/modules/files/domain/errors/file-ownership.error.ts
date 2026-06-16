import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when a user tries to confirm a file owned by another subject.
 */
export class FileOwnershipError extends DomainError {
  constructor(objectKey: string) {
    super({
      code: APP_ERROR_CODES.FORBIDDEN,
      kind: 'forbidden',
      userMessage: 'You cannot access this file.',
      internalMessage: 'File object key does not belong to the current user.',
      logContext: { objectKey }
    });
  }
}
