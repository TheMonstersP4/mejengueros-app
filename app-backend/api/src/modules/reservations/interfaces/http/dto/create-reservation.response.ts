import { ApiProperty } from '@nestjs/swagger';
import { ReservationStatus } from '@/generated/prisma/enums';

export class CreateReservationResponse {
  @ApiProperty({ example: 'reservation-id' })
  id!: string;

  @ApiProperty({ example: '38fad3d5-0f6a-4c8a-a49a-c3dce07af6cf' })
  courtId!: string;

  @ApiProperty({ example: '2026-07-01T18:00:00.000Z' })
  startsAt!: string;

  @ApiProperty({ example: '2026-07-01T19:00:00.000Z' })
  endsAt!: string;

  @ApiProperty({ enum: ReservationStatus, enumName: 'ReservationStatus' })
  status!: ReservationStatus;
}
