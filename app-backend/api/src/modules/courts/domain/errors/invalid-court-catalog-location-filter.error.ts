import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

export class InvalidCourtCatalogLocationFilterError extends DomainError {
  static cantonOutsideProvince(
    provinceId: string,
    cantonId: string
  ): InvalidCourtCatalogLocationFilterError {
    return new InvalidCourtCatalogLocationFilterError(
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
