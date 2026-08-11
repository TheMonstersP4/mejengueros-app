import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class InvalidReviewQuestionnaireError extends DomainError {
  constructor(logContext: Record<string, unknown>) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: 'Completá el cuestionario breve antes de enviar la reseña.',
      internalMessage: 'Review questionnaire answers are missing or invalid.',
      logContext
    });
  }
}
