import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an upload intent uses a MIME type outside the image policy.
 */
export class UnsupportedFileTypeError extends DomainError {
  constructor(contentType: string) {
    super({
      code: APP_ERROR_CODES.UNSUPPORTED_MEDIA_TYPE,
      kind: 'validation',
      userMessage: 'Unsupported image type.',
      internalMessage: `Unsupported image type: ${contentType}.`,
      logContext: { contentType },
      httpStatus: 415
    });
  }
}
