import { Controller, Get, Inject, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { ListOwnerReservationsUseCase } from '../../../application/use-cases/list-owner-reservations.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { ListOwnerReservationsQuery } from '../dto/list-owner-reservations.query';
import { OwnerReservationsResponse } from '../dto/owner-reservations.response';

/**
 * HTTP endpoints for the owner reservations screen.
 */
@ApiTags('owners')
@ApiBearerAuth()
@Controller('owners/me/reservations')
export class OwnerReservationsController {
  constructor(
    @Inject(ListOwnerReservationsUseCase)
    private readonly listOwnerReservations: ListOwnerReservationsUseCase
  ) {}

  @Get()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List reservations booked on courts owned by the authenticated owner.',
    description:
      'Returns the reservations booked by other players on every court in the authenticated owner complexes, optionally filtered by `courtId`. Reservations are grouped into upcoming and finalized sections, mirroring the My Reservations layout. Reservations the owner booked as a player are never included.'
  })
  @ApiEnvelopeOk(
    OwnerReservationsResponse,
    'Owner reservations wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 404)
  async list(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Query() query: ListOwnerReservationsQuery
  ): Promise<OwnerReservationsResponse> {
    return this.listOwnerReservations.execute(user, {
      courtId: query.courtId
    });
  }
}
