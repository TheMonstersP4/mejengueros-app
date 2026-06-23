import { Body, Controller, Inject, Post, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeCreated,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { CreateComplexWithFirstCourtUseCase } from '../../../application/use-cases/create-complex-with-first-court.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { CreateComplexRequest } from '../dto/create-complex.request';
import { CreateComplexResponse } from '../dto/create-complex.response';

/**
 * HTTP endpoints for sports complexes.
 */
@ApiTags('complexes')
@ApiBearerAuth()
@Controller('complexes')
export class ComplexesController {
  constructor(
    @Inject(CreateComplexWithFirstCourtUseCase)
    private readonly createComplexWithFirstCourt: CreateComplexWithFirstCourtUseCase
  ) {}

  /**
   * Creates a complex and its first court in a single backend action.
   */
  @Post()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Create a sports complex with its first court.',
    description:
      'Requires an authenticated user with OWNER role in the local database and creates both records atomically.'
  })
  @ApiBody({
    description: 'Required data to create the complex and its first court.',
    type: CreateComplexRequest
  })
  @ApiEnvelopeCreated(
    CreateComplexResponse,
    'Created complex and first court wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 403)
  async create(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Body() request: CreateComplexRequest
  ): Promise<CreateComplexResponse> {
    return this.createComplexWithFirstCourt.execute(user, request);
  }
}
