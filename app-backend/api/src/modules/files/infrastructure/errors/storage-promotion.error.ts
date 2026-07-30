import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { ErrorLogLevel } from '../../../../shared/domain/errors/base.error';
import { InfrastructureError } from '../../../../shared/infrastructure/errors/infrastructure.error';

/**
 * Raised when storage cannot promote a verified upload to durable storage.
 */
export class StoragePromotionError extends InfrastructureError {
  constructor(sourceObjectKey: string, destinationObjectKey: string, cause: unknown) {
    super({
      code: APP_ERROR_CODES.EXTERNAL_SERVICE_ERROR,
      kind: 'external',
      userMessage: 'Unable to confirm the uploaded file right now.',
      internalMessage: 'S3 failed while promoting an uploaded object.',
      logLevel: ErrorLogLevel.Error,
      logContext: { sourceObjectKey, destinationObjectKey },
      cause
    });
  }
}
