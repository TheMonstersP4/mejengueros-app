import { Controller, Get, Inject, Param, Patch, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import type { IAuthenticatedUserOutput } from '@/modules/auth/application/dto/authenticated-user.output';
import { CognitoAuthGuard } from '@/modules/auth/interfaces/http/guards/cognito-auth.guard';
import { CurrentUser } from '@/shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeArrayOk,
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '@/shared/interfaces/http/swagger/api-envelope.decorators';
import { ListMyNotificationsUseCase } from '../../../application/use-cases/list-my-notifications.use-case';
import { MarkNotificationReadUseCase } from '../../../application/use-cases/mark-notification-read.use-case';
import { NotificationResponse } from '../dto/notification.response';

@ApiTags('notifications')
@ApiBearerAuth()
@UseGuards(CognitoAuthGuard)
@Controller('notifications')
export class NotificationsController {
  constructor(
    @Inject(ListMyNotificationsUseCase)
    private readonly listMyNotifications: ListMyNotificationsUseCase,
    @Inject(MarkNotificationReadUseCase)
    private readonly markNotificationRead: MarkNotificationReadUseCase
  ) {}

  @Get()
  @ApiOperation({
    summary: 'List the authenticated user notifications.',
    description:
      'Returns persisted notifications for the authenticated player. Realtime WebSocket delivery is best-effort; this endpoint is the source of truth.'
  })
  @ApiEnvelopeArrayOk(
    NotificationResponse,
    'Authenticated user notifications wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(401)
  async list(
    @CurrentUser() user: IAuthenticatedUserOutput
  ): Promise<NotificationResponse[]> {
    return this.listMyNotifications.execute(user);
  }

  @Patch(':notificationId/read')
  @ApiOperation({
    summary: 'Mark one notification as read.',
    description:
      'Marks a notification as read only when it belongs to the authenticated user.'
  })
  @ApiEnvelopeOk(
    NotificationResponse,
    'Updated notification wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(401, 404)
  async markRead(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Param('notificationId') notificationId: string
  ): Promise<NotificationResponse> {
    return this.markNotificationRead.execute(user, notificationId);
  }
}
