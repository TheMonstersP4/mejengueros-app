import {
  Body,
  Controller,
  Get,
  Inject,
  Param,
  ParseUUIDPipe,
  Put,
  UseGuards
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiBody,
  ApiOperation,
  ApiParam,
  ApiTags
} from '@nestjs/swagger';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { GetCourtAvailabilityUseCase } from '../../../application/use-cases/get-court-availability.use-case';
import { SaveCourtAvailabilityUseCase } from '../../../application/use-cases/save-court-availability.use-case';
import { SaveCourtAvailabilityRequest } from '../dto/court-availability.request';
import { CourtAvailabilityResponse } from '../dto/court-availability.response';

@ApiTags('court-availability')
@ApiBearerAuth()
@Controller('courts/:courtId/availability')
export class CourtAvailabilityController {
  constructor(
    @Inject(GetCourtAvailabilityUseCase)
    private readonly getCourtAvailability: GetCourtAvailabilityUseCase,
    @Inject(SaveCourtAvailabilityUseCase)
    private readonly saveCourtAvailability: SaveCourtAvailabilityUseCase
  ) {}

  @Get()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Read one owned court availability.',
    description:
      'Returns the current shared reservable weekday set and shared time range for the authenticated owner court.'
  })
  @ApiParam({ name: 'courtId', description: 'Owned court identifier.', format: 'uuid' })
  @ApiEnvelopeOk(
    CourtAvailabilityResponse,
    'Court context and current availability wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 404)
  async get(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Param('courtId', new ParseUUIDPipe()) courtId: string
  ): Promise<CourtAvailabilityResponse> {
    return this.getCourtAvailability.execute(user, courtId);
  }

  @Put()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Create or update one owned court availability.',
    description:
      'Persists one shared whole-hour time range and one selected weekday set for the authenticated owner court.'
  })
  @ApiParam({ name: 'courtId', description: 'Owned court identifier.', format: 'uuid' })
  @ApiBody({ type: SaveCourtAvailabilityRequest })
  @ApiEnvelopeOk(
    CourtAvailabilityResponse,
    'Saved court availability wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 404)
  async save(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Param('courtId', new ParseUUIDPipe()) courtId: string,
    @Body() request: SaveCourtAvailabilityRequest
  ): Promise<CourtAvailabilityResponse> {
    return this.saveCourtAvailability.execute(user, courtId, request);
  }
}
