import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '@/modules/auth/application/dto/authenticated-user.output';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import type { INotificationOutput } from '../dto/notification.output';
import { presentNotification } from '../services/notification-presenter';
import {
  NOTIFICATION_REPOSITORY,
  type INotificationRepository
} from '../../domain/repositories/notification.repository';

/**
 * Lists notifications owned by the authenticated player.
 */
@Injectable()
export class ListMyNotificationsUseCase {
  constructor(
    @Inject(NOTIFICATION_REPOSITORY)
    private readonly notificationRepository: INotificationRepository,
    @Inject(SyncAuthenticatedUserUseCase)
    private readonly syncAuthenticatedUser: SyncAuthenticatedUserUseCase
  ) {}

  /**
   * Returns the current user's notifications ordered by newest first.
   *
   * @param identity - Authenticated Cognito identity.
   * @returns Notifications ready for client rendering.
   */
  async execute(identity: IAuthenticatedUserOutput): Promise<INotificationOutput[]> {
    const user = await this.syncAuthenticatedUser.execute(identity);
    const notifications = await this.notificationRepository.listForUser(user.id);

    return notifications.map(presentNotification);
  }
}
