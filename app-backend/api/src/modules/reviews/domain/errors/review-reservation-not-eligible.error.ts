import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class ReviewReservationNotEligibleError extends DomainError {
  constructor(reservationId: string, status: string) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: 'Solo se pueden reseñar reservas completadas.',
      internalMessage: 'Only completed reservations can be reviewed.',
      logContext: { reservationId, status }
    });
  }
}
