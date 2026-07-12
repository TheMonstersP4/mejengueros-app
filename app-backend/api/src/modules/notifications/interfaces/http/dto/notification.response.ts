import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import type { NotificationStatus, NotificationType } from '@/generated/prisma/enums';

/**
 * Action attached to a notification.
 */
export class NotificationActionResponse {
  @ApiProperty({
    description: 'Client action type.',
    example: 'OPEN_REVIEW'
  })
  type!: 'OPEN_REVIEW';

  @ApiProperty({
    description: 'Reservation that should be opened by the review flow.',
    example: '1a51d688-60ec-4d79-beb1-2d4932479f30'
  })
  reservationId!: string;
}

/**
 * Reservation context included in a notification.
 */
export class NotificationReservationResponse {
  @ApiProperty({
    description: 'Reservation identifier.',
    example: '1a51d688-60ec-4d79-beb1-2d4932479f30'
  })
  id!: string;

  @ApiProperty({
    description: 'Complex name where the reservation happened.',
    example: 'Mejengas CR'
  })
  complexName!: string;

  @ApiProperty({
    description: 'Court name where the reservation happened.',
    example: 'Cancha 1'
  })
  courtName!: string;

  @ApiProperty({
    description: 'Reservation start timestamp in ISO 8601 format.',
    example: '2026-07-11T18:00:00.000Z'
  })
  startsAt!: string;

  @ApiProperty({
    description: 'Reservation end timestamp in ISO 8601 format.',
    example: '2026-07-11T19:00:00.000Z'
  })
  endsAt!: string;
}

/**
 * User notification returned by the API.
 */
export class NotificationResponse {
  @ApiProperty({
    description: 'Notification identifier.',
    example: '40ee9673-8bd2-4090-bac1-1e92e9ef5c62'
  })
  id!: string;

  @ApiProperty({
    description: 'Notification type.',
    example: 'REVIEW_PROMPT'
  })
  type!: NotificationType;

  @ApiProperty({
    description: 'Notification status.',
    example: 'PENDING'
  })
  status!: NotificationStatus;

  @ApiProperty({
    description: 'Reservation related to the notification.',
    example: '1a51d688-60ec-4d79-beb1-2d4932479f30'
  })
  reservationId!: string;

  @ApiProperty({
    description: 'Short user-facing notification title.',
    example: 'Contanos como estuvo la mejenga'
  })
  title!: string;

  @ApiProperty({
    description: 'User-facing notification message.',
    example: 'Tu reserva en Mejengas CR - Cancha 1 ya termino. Dejanos tu resena.'
  })
  message!: string;

  @ApiProperty({
    description: 'Reservation context used to open the review flow.',
    type: NotificationReservationResponse
  })
  reservation!: NotificationReservationResponse;

  @ApiProperty({
    description: 'Client action that should run when the notification is tapped.',
    type: NotificationActionResponse
  })
  action!: NotificationActionResponse;

  @ApiProperty({
    description: 'Creation timestamp in ISO 8601 format.',
    example: '2026-07-11T18:30:00.000Z'
  })
  createdAt!: string;

  @ApiPropertyOptional({
    description: 'Read timestamp in ISO 8601 format.',
    nullable: true,
    example: null
  })
  readAt!: string | null;
}
