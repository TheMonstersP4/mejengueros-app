import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

export class InvalidCourtAvailabilityError extends DomainError {
  private constructor(userMessage: string, logContext?: Record<string, unknown>) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage,
      logContext
    });
  }

  static missingWeekdays(): InvalidCourtAvailabilityError {
    return new InvalidCourtAvailabilityError(
      'Court availability requires at least one weekday.'
    );
  }

  static invalidRange(startTime: string, endTime: string): InvalidCourtAvailabilityError {
    return new InvalidCourtAvailabilityError(
      'Court availability must use one shared whole-hour range that produces one-hour slots.',
      {
        startTime,
        endTime
      }
    );
  }
}
