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
import { GetReservableSlotsUseCase } from '../../../application/use-cases/get-reservable-slots.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { GetReservableSlotsRequest } from '../dto/reservable-slots.request';
import { ReservableSlotsResponse } from '../dto/reservable-slots.response';

@ApiTags('reservations')
@ApiBearerAuth()
@Controller('courts/:courtId/reservable-slots')
export class ReservableSlotsController {
  constructor(
    @Inject(GetReservableSlotsUseCase)
    private readonly getReservableSlots: GetReservableSlotsUseCase
  ) {}

  @Get()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List reservable one-hour slots for one court date.',
    description:
      'Returns UTC one-hour reservation slot candidates derived from Costa Rica business-time court availability for the selected date, excluding already confirmed reservations and any same-day slots whose start time is less than or equal to the 30-minute minimum advance threshold using the America/Costa_Rica civil date.'
  })
  @ApiParam({ name: 'courtId', description: 'Court identifier.', format: 'uuid' })
  @ApiQuery({
    name: 'date',
    description: 'Reservation date in Costa Rica business YYYY-MM-DD format.'
  })
  @ApiEnvelopeOk(
    ReservableSlotsResponse,
    'Reservable slot summary wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 404)
  async get(
    @Param('courtId', new ParseUUIDPipe()) courtId: string,
    @Query() query: GetReservableSlotsRequest
  ): Promise<ReservableSlotsResponse> {
    return this.getReservableSlots.execute(courtId, query.date);
  }
}
