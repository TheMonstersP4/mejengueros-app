import { ApiProperty } from '@nestjs/swagger';
import { CourtStatus } from '@/generated/prisma/enums';

export class ReservableDayResponse {
  @ApiProperty({ example: '2026-07-05' })
  date!: string;

  @ApiProperty({
    enum: ['AVAILABLE'],
    example: 'AVAILABLE'
  })
  availabilityStatus!: 'AVAILABLE';

  @ApiProperty({ example: 4, minimum: 1 })
  availableSlotsCount!: number;
}

export class ReservableDaysCourtResponse {
  @ApiProperty({ example: '38fad3d5-0f6a-4c8a-a49a-c3dce07af6cf' })
  id!: string;

  @ApiProperty({ example: 'Cancha 1' })
  name!: string;

  @ApiProperty({ enum: CourtStatus, enumName: 'CourtStatus' })
  status!: CourtStatus;
}

export class ReservableDaysResponse {
  @ApiProperty({ type: ReservableDaysCourtResponse })
  court!: ReservableDaysCourtResponse;

  @ApiProperty({ example: '2026-07-04' })
  from!: string;

  @ApiProperty({ example: 14, minimum: 1 })
  days!: number;

  @ApiProperty({
    type: [ReservableDayResponse],
    description:
      'Upcoming UTC dates that still have at least one future reservable one-hour slot after applying court availability, confirmed reservation filtering, and the same-day 30-minute minimum advance threshold.'
  })
  reservableDays!: ReservableDayResponse[];
}
