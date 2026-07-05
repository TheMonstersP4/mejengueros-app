import { ApiProperty } from '@nestjs/swagger';

/**
 * Public HTTP response card for the My Reservations screen.
 */
export class ReservationCardResponse {
  @ApiProperty({ example: 'reservation-id' })
  id!: string;

  @ApiProperty({ example: 'Moravia FC' })
  complexName!: string;

  @ApiProperty({ example: 'Cancha A' })
  courtName!: string;

  @ApiProperty({ required: false, example: 'https://signed.example.test/court-a.png' })
  imageUrl?: string;

  @ApiProperty({ example: '2026-07-10T18:00:00.000Z' })
  startsAt!: string;

  @ApiProperty({ example: '2026-07-10T19:00:00.000Z' })
  endsAt!: string;

  @ApiProperty({ enum: ['CONFIRMED', 'COMPLETED'], example: 'CONFIRMED' })
  status!: 'CONFIRMED' | 'COMPLETED';

  @ApiProperty({ enum: ['UPCOMING', 'FINALIZED'], example: 'UPCOMING' })
  section!: 'UPCOMING' | 'FINALIZED';

  @ApiProperty({
    enum: ['NOT_APPLICABLE', 'PENDING_REVIEW', 'REVIEWED'],
    example: 'PENDING_REVIEW'
  })
  reviewStatus!: 'NOT_APPLICABLE' | 'PENDING_REVIEW' | 'REVIEWED';

  @ApiProperty({ example: true })
  canReview!: boolean;

  @ApiProperty({ example: false })
  hasReview!: boolean;

  @ApiProperty({ required: false, enum: ['leave_review'], example: 'leave_review' })
  primaryActionKey?: 'leave_review';

  @ApiProperty({ required: false, example: 'Dejar reseña' })
  primaryActionLabel?: 'Dejar reseña';

  @ApiProperty({ required: false, enum: ['already_reviewed'], example: 'already_reviewed' })
  indicatorKey?: 'already_reviewed';

  @ApiProperty({ required: false, example: 'Ya dejaste tu reseña' })
  indicatorLabel?: 'Ya dejaste tu reseña';
}

/**
 * Public grouped response returned by `GET /v1/reservations/my`.
 */
export class MyReservationsResponse {
  @ApiProperty({
    type: [ReservationCardResponse],
    description:
      'Bounded upcoming cards snapshot for the My Reservations screen. Returns up to 20 cards.'
  })
  upcoming!: ReservationCardResponse[];

  @ApiProperty({
    type: [ReservationCardResponse],
    description:
      'Bounded finalized cards snapshot for the My Reservations screen. Returns up to 20 cards.'
  })
  finalized!: ReservationCardResponse[];
}
