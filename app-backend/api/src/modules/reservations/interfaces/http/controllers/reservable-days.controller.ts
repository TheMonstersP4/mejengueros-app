import {
  Controller,
  Get,
  Inject,
  Param,
  ParseUUIDPipe,
  Query,
  UseGuards
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiParam,
  ApiQuery,
  ApiTags
} from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import {
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { GetReservableDaysUseCase } from '../../../application/use-cases/get-reservable-days.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { GetReservableDaysRequest, MAX_RESERVABLE_DAYS_RANGE } from '../dto/reservable-days.request';
import { ReservableDaysResponse } from '../dto/reservable-days.response';

@ApiTags('reservations')
@ApiBearerAuth()
@Controller('courts/:courtId/reservable-days')
export class ReservableDaysController {
  constructor(
    @Inject(GetReservableDaysUseCase)
    private readonly getReservableDays: GetReservableDaysUseCase
  ) {}

  @Get()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List upcoming court dates that still have reservable one-hour slots.',
    description:
      'Returns only UTC calendar dates that currently have at least one future one-hour reservation slot after applying court status, weekday availability, existing confirmed reservations, and same-day started-slot filtering.'
  })
  @ApiParam({ name: 'courtId', description: 'Court identifier.', format: 'uuid' })
  @ApiQuery({
    name: 'from',
    description: 'Inclusive UTC start date for the discovery window in YYYY-MM-DD format.'
  })
  @ApiQuery({
    name: 'days',
    required: false,
    description: `Inclusive scan window length in days. Defaults to 14 and must be between 1 and ${MAX_RESERVABLE_DAYS_RANGE}.`,
    example: 14
  })
  @ApiEnvelopeOk(
    ReservableDaysResponse,
    'Reservable day discovery summary wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 404)
  async get(
    @Param('courtId', new ParseUUIDPipe()) courtId: string,
    @Query() query: GetReservableDaysRequest
  ): Promise<ReservableDaysResponse> {
    return this.getReservableDays.execute(courtId, query.from, query.days);
  }
}
