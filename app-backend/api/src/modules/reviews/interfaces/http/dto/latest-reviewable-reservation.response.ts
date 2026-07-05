import { ApiProperty } from '@nestjs/swagger';

export class LatestReviewableReservationResponse {
  @ApiProperty({ example: 'reservation-id' })
  reservationId!: string;

  @ApiProperty({ example: 'Moravia FC' })
  complexName!: string;

  @ApiProperty({ example: 'Cancha A' })
  courtName!: string;

  @ApiProperty({ example: '2026-07-02T20:00:00.000Z' })
  startsAt!: string;

  @ApiProperty({ example: '2026-07-02T21:00:00.000Z' })
  endsAt!: string;

  @ApiProperty({ required: false, example: 'https://signed.example.test/court-a.png' })
  imageUrl?: string;
}
