import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class OneStarReviewEvidenceRequiredError extends DomainError {
  constructor() {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage:
        'Las reseñas de 1 estrella deben incluir al menos una imagen como evidencia.',
      internalMessage: 'One-star reviews require at least one evidence image.',
      logContext: { rating: 1 }
    });
  }
}
