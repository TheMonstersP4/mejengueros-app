import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

export class CourtNotReservableError extends DomainError {
  private constructor(userMessage: string, logContext?: Record<string, unknown>) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage,
      logContext
    });
  }

  static inactive(courtId: string): CourtNotReservableError {
    return new CourtNotReservableError('Court is not available for reservations.', {
      courtId
    });
  }

  static unavailableDay(date: string, weekday: string): CourtNotReservableError {
    return new CourtNotReservableError(
      'Court is not reservable on the selected date.',
      { date, weekday }
    );
  }

  static missingAvailability(courtId: string): CourtNotReservableError {
    return new CourtNotReservableError('Court does not have reservable availability configured.', {
      courtId
    });
  }

  static outsideAvailability(
    startsAt: string,
    startTime: string,
    endTime: string
  ): CourtNotReservableError {
    return new CourtNotReservableError(
      'Reservation start time must fit within the configured one-hour court availability window.',
      { startsAt, startTime, endTime }
    );
  }
}
