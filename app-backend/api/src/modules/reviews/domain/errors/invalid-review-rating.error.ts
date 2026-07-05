import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class InvalidReviewRatingError extends DomainError {
  constructor(rating: number) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: 'La calificación debe estar entre 1 y 5 estrellas.',
      internalMessage: 'Review rating must be between 1 and 5.',
      logContext: { rating }
    });
  }
}
