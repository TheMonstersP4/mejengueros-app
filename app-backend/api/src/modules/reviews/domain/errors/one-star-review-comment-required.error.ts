import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class OneStarReviewCommentRequiredError extends DomainError {
  constructor() {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage:
        'Las reseñas de 1 estrella deben incluir un comentario que explique qué pasó.',
      internalMessage: 'One-star reviews require a non-blank comment.',
      logContext: { rating: 1 }
    });
  }
}
