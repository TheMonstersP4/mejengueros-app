import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

export class ReservationConflictError extends DomainError {
  constructor(courtId: string, startsAt: string) {
    super({
      code: APP_ERROR_CODES.CONFLICT,
      kind: 'conflict',
      userMessage: 'This court already has a confirmed reservation for the selected start time.',
      logContext: { courtId, startsAt }
    });
  }
}
