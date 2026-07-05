import { Transform } from 'class-transformer';
import { ApiProperty } from '@nestjs/swagger';
import { IsUUID, ValidateBy, type ValidationOptions } from 'class-validator';
import {
  parseUtcReservationStartsAt,
  UTC_RESERVATION_STARTS_AT_MESSAGE,
  UTC_RESERVATION_STARTS_AT_SCHEMA_PATTERN
} from '../../../shared/utc-reservation-starts-at';

function trimText(value: unknown): unknown {
  return typeof value === 'string' ? value.trim() : value;
}

function IsUtcReservationStartsAt(validationOptions?: ValidationOptions): PropertyDecorator {
  return ValidateBy(
    {
      name: 'isUtcReservationStartsAt',
      validator: {
        validate: (value: unknown) =>
          typeof value === 'string' && parseUtcReservationStartsAt(value) != null,
        defaultMessage: () => UTC_RESERVATION_STARTS_AT_MESSAGE
      }
    },
    validationOptions
  );
}

export class CreateReservationRequest {
  @ApiProperty({
    description: 'Reservable court identifier.',
    example: '38fad3d5-0f6a-4c8a-a49a-c3dce07af6cf'
  })
  @Transform(({ value }) => trimText(value))
  @IsUUID()
  courtId!: string;

  @ApiProperty({
    description:
      'Reservation start time as a real UTC ISO datetime with explicit Z aligned to a whole hour. Same-day reservations must start more than 30 minutes after the current UTC time.',
    example: '2026-07-01T18:00:00.000Z',
    format: 'date-time',
    pattern: UTC_RESERVATION_STARTS_AT_SCHEMA_PATTERN
  })
  @Transform(({ value }) => trimText(value))
  @IsUtcReservationStartsAt()
  startsAt!: string;
}
