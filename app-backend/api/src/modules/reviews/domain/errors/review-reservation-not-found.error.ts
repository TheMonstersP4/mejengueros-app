import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class ReviewReservationNotFoundError extends DomainError {
  constructor(reservationId: string) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'No encontramos la reserva indicada para dejar la reseña.',
      internalMessage: 'Review reservation was not found for the authenticated user.',
      logContext: { reservationId }
    });
  }
}
