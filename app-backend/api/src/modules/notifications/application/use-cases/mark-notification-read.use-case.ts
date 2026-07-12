import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '@/modules/auth/application/dto/authenticated-user.output';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import type { INotificationOutput } from '../dto/notification.output';
import { presentNotification } from '../services/notification-presenter';
import { NotificationNotFoundError } from '../../domain/errors/notification-not-found.error';
import {
  NOTIFICATION_REPOSITORY,
  type INotificationRepository
} from '../../domain/repositories/notification.repository';

/**
 * Marks a user notification as read.
 */
@Injectable()
export class MarkNotificationReadUseCase {
  constructor(
    @Inject(NOTIFICATION_REPOSITORY)
    private readonly notificationRepository: INotificationRepository,
    @Inject(SyncAuthenticatedUserUseCase)
    private readonly syncAuthenticatedUser: SyncAuthenticatedUserUseCase
  ) {}

  /**
   * Marks the notification as read when it belongs to the authenticated user.
   *
   * @param identity - Authenticated Cognito identity.
   * @param notificationId - Notification identifier.
   * @returns Updated notification.
   * @throws NotificationNotFoundError when the notification is absent or belongs to another user.
   */
  async execute(
    identity: IAuthenticatedUserOutput,
    notificationId: string
  ): Promise<INotificationOutput> {
    const user = await this.syncAuthenticatedUser.execute(identity);
    const notification = await this.notificationRepository.markRead(
      notificationId,
      user.id
    );

    if (notification == null) {
      throw new NotificationNotFoundError(notificationId);
    }

    return presentNotification(notification);
  }
}
