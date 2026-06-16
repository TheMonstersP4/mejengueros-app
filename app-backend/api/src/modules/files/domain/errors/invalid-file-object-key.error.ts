import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when a file object key is not valid for application-managed files.
 */
export class InvalidFileObjectKeyError extends DomainError {
  constructor(objectKey: string) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: 'Invalid file reference.',
      internalMessage: 'File object key has an invalid format.',
      logContext: { objectKey }
    });
  }
}
