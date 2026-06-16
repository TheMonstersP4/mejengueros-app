import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { ErrorLogLevel } from '../../../../shared/domain/errors/base.error';
import { InfrastructureError } from '../../../../shared/infrastructure/errors/infrastructure.error';

/**
 * Raised when uploaded image metadata cannot be persisted because DB is disabled.
 */
export class ImageUploadPersistenceUnavailableError extends InfrastructureError {
  constructor() {
    super({
      code: APP_ERROR_CODES.SERVICE_UNAVAILABLE,
      kind: 'external',
      userMessage: 'Image metadata storage is not available right now.',
      internalMessage: 'DATABASE_URL is required for image metadata endpoints.',
      logLevel: ErrorLogLevel.Error,
      httpStatus: 503
    });
  }
}
