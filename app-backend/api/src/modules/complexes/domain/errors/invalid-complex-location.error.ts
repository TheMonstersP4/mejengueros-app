import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when the selected wizard location is not a valid controlled catalog combination.
 */
export class InvalidComplexLocationError extends DomainError {
  static invalidProvince(provinceId: string): InvalidComplexLocationError {
    return new InvalidComplexLocationError(
      `Selected province "${provinceId}" does not exist in the controlled catalog.`,
      { provinceId }
    );
  }

  static invalidCanton(
    provinceId: string,
    cantonId: string
  ): InvalidComplexLocationError {
    return new InvalidComplexLocationError(
      `Selected canton "${cantonId}" does not belong to province "${provinceId}".`,
      { provinceId, cantonId }
    );
  }

  private constructor(userMessage: string, logContext: Record<string, unknown>) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage,
      logContext
    });
  }
}
