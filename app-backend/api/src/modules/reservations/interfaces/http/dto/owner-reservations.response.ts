import { ApiProperty } from '@nestjs/swagger';

/**
 * Public HTTP response card for the Owner Reservations screen.
 */
export class OwnerReservationCardResponse {
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
}

/**
 * Public grouped response returned by `GET /v1/owners/me/reservations`.
 */
export class OwnerReservationsResponse {
  @ApiProperty({
    description:
      'Court filter applied to the response, or null when listing every owned court.',
    example: '0dd3a274-7d7b-45c6-a90d-4d14298ae7aa',
    nullable: true
  })
  selectedCourtId!: string | null;

  @ApiProperty({
    type: [OwnerReservationCardResponse],
    description:
      'Bounded upcoming cards booked on the owner courts. Returns up to 20 cards.'
  })
  upcoming!: OwnerReservationCardResponse[];

  @ApiProperty({
    type: [OwnerReservationCardResponse],
    description:
      'Bounded finalized cards booked on the owner courts. Returns up to 20 cards.'
  })
  finalized!: OwnerReservationCardResponse[];
}
