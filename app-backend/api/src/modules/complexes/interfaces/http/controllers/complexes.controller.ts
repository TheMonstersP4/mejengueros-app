import { Body, Controller, Get, Inject, Post, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeCreated,
  ApiEnvelopeOk,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { CreateComplexWithFirstCourtUseCase } from '../../../application/use-cases/create-complex-with-first-court.use-case';
import { GetMyComplexHubUseCase } from '../../../application/use-cases/get-my-complex-hub.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { CreateComplexRequest } from '../dto/create-complex.request';
import { CreateComplexResponse } from '../dto/create-complex.response';
import { MyComplexHubResponse } from '../dto/my-complex-hub.response';

/**
 * HTTP endpoints for sports complexes.
 */
@ApiTags('complexes')
@ApiBearerAuth()
@Controller('complexes')
export class ComplexesController {
  constructor(
    @Inject(CreateComplexWithFirstCourtUseCase)
    private readonly createComplexWithFirstCourt: CreateComplexWithFirstCourtUseCase,
    @Inject(GetMyComplexHubUseCase)
    private readonly getMyComplexHub: GetMyComplexHubUseCase
  ) {}

  @Get('my-hub')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Read the authenticated owner complex hub.',
    description:
      'Returns only the complexes and courts owned by the authenticated user, including whether each court availability is already configured.'
  })
  @ApiEnvelopeOk(
    MyComplexHubResponse,
    'Owner complexes and courts wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(401)
  async getMyHub(@CurrentUser() user: IAuthenticatedUserOutput): Promise<MyComplexHubResponse> {
    return this.getMyComplexHub.execute(user);
  }

  /**
   * Creates a complex and its first court in a single backend action.
   */
  @Post()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Create a sports complex with its first court.',
    description:
      'Requires an authenticated user, creates the complex and first court atomically, and persists the local OWNER role for the creator when needed.'
  })
  @ApiBody({
    description: 'Required data to create the complex and its first court.',
    type: CreateComplexRequest
  })
  @ApiEnvelopeCreated(
    CreateComplexResponse,
    'Created complex and first court wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401)
  async create(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Body() request: CreateComplexRequest
  ): Promise<CreateComplexResponse> {
    return this.createComplexWithFirstCourt.execute(user, request);
  }
}
