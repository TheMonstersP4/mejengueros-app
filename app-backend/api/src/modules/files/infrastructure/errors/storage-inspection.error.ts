import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { ErrorLogLevel } from '../../../../shared/domain/errors/base.error';
import { InfrastructureError } from '../../../../shared/infrastructure/errors/infrastructure.error';

/**
 * Raised when the storage provider cannot inspect an uploaded object.
 */
export class StorageInspectionError extends InfrastructureError {
  constructor(objectKey: string, cause: unknown) {
    super({
      code: APP_ERROR_CODES.EXTERNAL_SERVICE_ERROR,
      kind: 'external',
      userMessage: 'Unable to inspect the uploaded file right now.',
      internalMessage: 'S3 failed while inspecting an uploaded object.',
      logLevel: ErrorLogLevel.Error,
      logContext: { objectKey },
      cause
    });
  }
}
