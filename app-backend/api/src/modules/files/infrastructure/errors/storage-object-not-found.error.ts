import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { InfrastructureError } from '../../../../shared/infrastructure/errors/infrastructure.error';

/**
 * Raised when the storage provider cannot find an uploaded object.
 */
export class StorageObjectNotFoundError extends InfrastructureError {
  constructor(objectKey: string, cause?: unknown) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'Uploaded file was not found.',
      internalMessage: 'S3 object was not found during upload confirmation.',
      logContext: { objectKey },
      cause
    });
  }
}
