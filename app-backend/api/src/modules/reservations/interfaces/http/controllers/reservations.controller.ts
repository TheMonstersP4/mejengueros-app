import { Body, Controller, Inject, Post, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeCreated,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { CreateReservationUseCase } from '../../../application/use-cases/create-reservation.use-case';
import { CreateReservationRequest } from '../dto/create-reservation.request';
import { CreateReservationResponse } from '../dto/create-reservation.response';

@ApiTags('reservations')
@ApiBearerAuth()
@Controller('reservations')
export class ReservationsController {
  constructor(
    @Inject(CreateReservationUseCase)
    private readonly createReservation: CreateReservationUseCase
  ) {}

  @Post()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Create one confirmed reservation.',
    description:
      'Creates a one-hour confirmed reservation for the authenticated player when the selected court slot is reservable and not already booked.'
  })
  @ApiBody({ type: CreateReservationRequest })
  @ApiEnvelopeCreated(
    CreateReservationResponse,
    'Created reservation wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 404, 409)
  async create(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Body() request: CreateReservationRequest
  ): Promise<CreateReservationResponse> {
    return this.createReservation.execute(user, request);
  }
}
