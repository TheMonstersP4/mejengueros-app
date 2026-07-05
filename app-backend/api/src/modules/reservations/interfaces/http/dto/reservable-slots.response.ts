import { ApiProperty } from '@nestjs/swagger';
import { CourtStatus } from '@/generated/prisma/enums';

export class ReservableSlotResponse {
  @ApiProperty({ example: '2026-07-01T18:00:00.000Z' })
  startsAt!: string;

  @ApiProperty({ example: '2026-07-01T19:00:00.000Z' })
  endsAt!: string;
}

export class ReservableSlotsCourtResponse {
  @ApiProperty({ example: '38fad3d5-0f6a-4c8a-a49a-c3dce07af6cf' })
  id!: string;

  @ApiProperty({ example: 'Cancha 1' })
  name!: string;

  @ApiProperty({ enum: CourtStatus, enumName: 'CourtStatus' })
  status!: CourtStatus;
}

export class ReservableSlotsResponse {
  @ApiProperty({ type: ReservableSlotsCourtResponse })
  court!: ReservableSlotsCourtResponse;

  @ApiProperty({ example: '2026-07-01' })
  date!: string;

  @ApiProperty({
    enum: ['AVAILABLE', 'FULLY_BOOKED', 'UNAVAILABLE'],
    example: 'AVAILABLE'
  })
  availabilityStatus!: 'AVAILABLE' | 'FULLY_BOOKED' | 'UNAVAILABLE';

  @ApiProperty({
    type: [ReservableSlotResponse],
    description:
      'UTC one-hour slot candidates after excluding confirmed reservations and any current-day slots that do not clear the 30-minute minimum advance threshold.'
  })
  slots!: ReservableSlotResponse[];
}
