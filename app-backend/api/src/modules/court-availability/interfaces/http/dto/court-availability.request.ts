import { ApiProperty } from '@nestjs/swagger';
import { ArrayNotEmpty, ArrayUnique, IsArray, IsEnum, Matches } from 'class-validator';
import { Weekday } from '@/generated/prisma/enums';

const WHOLE_HOUR_PATTERN = /^(?:[01]\d|2[0-3]):00$/;

export class SaveCourtAvailabilityRequest {
  @ApiProperty({
    description: 'Selected reservable weekdays for this court.',
    enum: Weekday,
    enumName: 'Weekday',
    isArray: true,
    example: [Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.FRIDAY]
  })
  @IsArray()
  @ArrayNotEmpty()
  @ArrayUnique()
  @IsEnum(Weekday, { each: true })
  days!: Weekday[];

  @ApiProperty({
    description: 'Shared court opening time in whole-hour format.',
    example: '06:00'
  })
  @Matches(WHOLE_HOUR_PATTERN)
  startTime!: string;

  @ApiProperty({
    description: 'Shared court closing time in whole-hour format.',
    example: '22:00'
  })
  @Matches(WHOLE_HOUR_PATTERN)
  endTime!: string;
}
