import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when the requested court is outside the authenticated owner's
 * managed complexes. Surfaces as 404 to avoid leaking existence.
 */
export class OwnerReservationCourtNotAccessibleError extends DomainError {
  constructor(courtId: string) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'Court not found for the authenticated owner.',
      logContext: { courtId }
    });
  }
}
