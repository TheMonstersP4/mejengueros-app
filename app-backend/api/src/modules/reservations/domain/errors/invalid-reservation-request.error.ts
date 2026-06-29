import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

export class InvalidReservationRequestError extends DomainError {
  private constructor(userMessage: string, logContext?: Record<string, unknown>) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage,
      logContext
    });
  }

  static invalidDate(date: string): InvalidReservationRequestError {
    return new InvalidReservationRequestError('Date must use a real YYYY-MM-DD calendar date.', {
      date
    });
  }

  static invalidStartsAt(startsAt: string): InvalidReservationRequestError {
    return new InvalidReservationRequestError(
      'Reservation start time must be a real UTC ISO datetime with explicit Z aligned to a whole hour.',
      { startsAt }
    );
  }

  static startedAtOrBeforeNow(
    startsAt: string,
    now: string
  ): InvalidReservationRequestError {
    return new InvalidReservationRequestError(
      'Reservation start time must be strictly in the future.',
      { startsAt, now }
    );
  }
}
