import { Transform, Type } from 'class-transformer';
import { ApiPropertyOptional, ApiProperty } from '@nestjs/swagger';
import {
  IsInt,
  Max,
  Min,
  ValidateBy,
  type ValidationOptions
} from 'class-validator';
import { parseDateOnly } from '../../../domain/services/reservation-slot-policy';

export const DEFAULT_RESERVABLE_DAYS_RANGE = 14;
export const MAX_RESERVABLE_DAYS_RANGE = 31;

function trimText(value: unknown): unknown {
  return typeof value === 'string' ? value.trim() : value;
}

function IsDateOnly(validationOptions?: ValidationOptions): PropertyDecorator {
  return ValidateBy(
    {
      name: 'isDateOnly',
      validator: {
        validate: (value: unknown) => {
          if (typeof value !== 'string') {
            return false;
          }

          try {
            parseDateOnly(value);
            return true;
          } catch {
            return false;
          }
        },
        defaultMessage: () => 'Date must use a real YYYY-MM-DD calendar date.'
      }
    },
    validationOptions
  );
}

export class GetReservableDaysRequest {
  @ApiProperty({
    description: 'Inclusive UTC start date for the discovery window in YYYY-MM-DD format.',
    example: '2026-07-04'
  })
  @Transform(({ value }) => trimText(value))
  @IsDateOnly()
  from!: string;

  @ApiPropertyOptional({
    description:
      'Number of UTC calendar days to scan from the inclusive start date. Days above the safe bounded limit are rejected.',
    example: DEFAULT_RESERVABLE_DAYS_RANGE,
    minimum: 1,
    maximum: MAX_RESERVABLE_DAYS_RANGE,
    default: DEFAULT_RESERVABLE_DAYS_RANGE
  })
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(MAX_RESERVABLE_DAYS_RANGE)
  days: number = DEFAULT_RESERVABLE_DAYS_RANGE;
}
