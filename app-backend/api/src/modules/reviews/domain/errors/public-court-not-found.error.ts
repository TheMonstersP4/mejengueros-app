import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

/**
 * Raised when public court reviews are requested for a court that does not
 * exist or is not publicly visible. Surfaces as 404 to avoid leaking the
 * existence of unpublished or deleted courts.
 */
export class PublicCourtReviewsCourtNotFoundError extends DomainError {
  constructor(courtId: string) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'No encontramos la cancha indicada.',
      internalMessage:
        'Public court reviews were requested for a court that is not publicly visible.',
      logContext: { courtId }
    });
  }
}
