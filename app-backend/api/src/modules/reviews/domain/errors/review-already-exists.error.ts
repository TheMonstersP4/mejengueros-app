import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class ReviewAlreadyExistsError extends DomainError {
  constructor(reservationId: string) {
    super({
      code: APP_ERROR_CODES.CONFLICT,
      kind: 'conflict',
      userMessage: 'Esta reserva ya tiene una reseña registrada.',
      internalMessage: 'Reservation already has a review.',
      logContext: { reservationId }
    });
  }
}
