import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

/**
 * Raised when a notification cannot be found in the authenticated user's scope.
 */
export class NotificationNotFoundError extends DomainError {
  constructor(notificationId: string) {
    super({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'No encontramos esa notificacion.',
      internalMessage: 'Notification lookup returned no result for the user scope.',
      logContext: { notificationId }
    });

    this.name = 'NotificationNotFoundError';
  }
}
