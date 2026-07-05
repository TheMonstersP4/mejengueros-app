import { Transform } from 'class-transformer';
import { ApiProperty } from '@nestjs/swagger';
import { Matches } from 'class-validator';

const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

function trimText(value: unknown): unknown {
  return typeof value === 'string' ? value.trim() : value;
}

export class GetReservableSlotsRequest {
  @ApiProperty({
    description:
      'Reservation date in UTC calendar format. When the date is today in UTC, only slots with a start time more than 30 minutes in the future are returned.',
    example: '2026-07-01'
  })
  @Transform(({ value }) => trimText(value))
  @Matches(DATE_ONLY_PATTERN)
  date!: string;
}
