import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an upload intent uses an unsupported file purpose.
 */
export class InvalidFilePurposeError extends DomainError {
  constructor(purpose: string) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: 'Unsupported file purpose.',
      internalMessage: `Unsupported file purpose: ${purpose}.`,
      logContext: { purpose }
    });
  }
}
