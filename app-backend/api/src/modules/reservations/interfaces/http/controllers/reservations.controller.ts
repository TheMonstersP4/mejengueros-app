import { Body, Controller, Get, Inject, Post, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeOk,
  ApiEnvelopeCreated,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { CreateReservationUseCase } from '../../../application/use-cases/create-reservation.use-case';
import { ListMyReservationsUseCase } from '../../../application/use-cases/list-my-reservations.use-case';
import { CreateReservationRequest } from '../dto/create-reservation.request';
import { CreateReservationResponse } from '../dto/create-reservation.response';
import { MyReservationsResponse } from '../dto/my-reservations.response';

@ApiTags('reservations')
@ApiBearerAuth()
@Controller('reservations')
export class ReservationsController {
  constructor(
    @Inject(CreateReservationUseCase)
    private readonly createReservation: CreateReservationUseCase,
    @Inject(ListMyReservationsUseCase)
    private readonly listMyReservations: ListMyReservationsUseCase
  ) {}

  @Get('my')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List the authenticated player reservations grouped for the My Reservations screen.',
    description:
      'Returns a bounded screen snapshot for the authenticated player with render-ready reservation cards, including review call-to-action and indicator state so clients do not infer review rules. The response contains up to 20 upcoming cards and up to 20 finalized cards.'
  })
  @ApiEnvelopeOk(
    MyReservationsResponse,
    'Grouped authenticated player reservations wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(401)
  async my(@CurrentUser() user: IAuthenticatedUserOutput): Promise<MyReservationsResponse> {
    return this.listMyReservations.execute(user);
  }

  @Post()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Create one confirmed reservation.',
    description:
      'Creates a one-hour confirmed reservation for the authenticated player when the selected court slot is reservable, not already booked, and satisfies the same-day 30-minute minimum advance threshold.'
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
