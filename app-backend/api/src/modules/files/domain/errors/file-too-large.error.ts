import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an upload intent exceeds the configured size limit.
 */
export class FileTooLargeError extends DomainError {
  constructor(sizeBytes: number, maxSizeBytes: number) {
    super({
      code: APP_ERROR_CODES.PAYLOAD_TOO_LARGE,
      kind: 'validation',
      userMessage: 'File is too large.',
      internalMessage: `File size ${sizeBytes} exceeds ${maxSizeBytes} bytes.`,
      logContext: { sizeBytes, maxSizeBytes },
      httpStatus: 413
    });
  }
}
