import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Weekday } from '@/generated/prisma/enums';

export class CourtAvailabilityCourtResponse {
  @ApiProperty({ example: 'court-id' })
  id!: string;

  @ApiProperty({ example: 'Cancha 1' })
  name!: string;

  @ApiProperty({ example: 'complex-id' })
  complexId!: string;

  @ApiProperty({ example: 'Mejengas CR' })
  complexName!: string;
}

export class CourtAvailabilityConfigResponse {
  @ApiProperty({
    enum: Weekday,
    enumName: 'Weekday',
    isArray: true,
    example: [Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.FRIDAY]
  })
  days!: Weekday[];

  @ApiProperty({ example: '06:00' })
  startTime!: string;

  @ApiProperty({ example: '22:00' })
  endTime!: string;
}

export class CourtAvailabilityResponse {
  @ApiProperty({ type: CourtAvailabilityCourtResponse })
  court!: CourtAvailabilityCourtResponse;

  @ApiPropertyOptional({
    type: CourtAvailabilityConfigResponse,
    nullable: true
  })
  availability!: CourtAvailabilityConfigResponse | null;
}
