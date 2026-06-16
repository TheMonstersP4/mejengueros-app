import { InfrastructureError } from '../../../../shared/infrastructure/errors/infrastructure.error';
import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { ErrorLogLevel } from '../../../../shared/domain/errors/base.error';

/**
 * Wraps provider failures while creating upload URLs.
 */
export class StorageUploadUrlError extends InfrastructureError {
  constructor(cause: unknown) {
    super({
      code: APP_ERROR_CODES.EXTERNAL_SERVICE_ERROR,
      kind: 'external',
      userMessage: 'Unable to create an upload URL right now.',
      internalMessage: 'S3 failed to create a presigned upload URL.',
      logLevel: ErrorLogLevel.Error,
      cause
    });
  }
}
