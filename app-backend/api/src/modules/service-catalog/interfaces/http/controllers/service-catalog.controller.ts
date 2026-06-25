import { Controller, Get, Inject, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiQuery, ApiTags } from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import {
  ApiEnvelopeArrayOk,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { ListActiveServicesUseCase } from '../../../application/use-cases/list-active-services.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { SERVICE_SCOPE_VALUES, ListServicesQuery } from '../dto/list-services.query';
import { ServiceCatalogResponse } from '../dto/service-catalog.response';

/**
 * HTTP endpoints for active wizard service catalogs.
 */
@ApiTags('services')
@ApiBearerAuth()
@Controller('services')
export class ServiceCatalogController {
  constructor(
    @Inject(ListActiveServicesUseCase)
    private readonly listActiveServices: ListActiveServicesUseCase
  ) {}

  @Get()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List active services for the complex wizard.',
    description:
      'Returns active service catalog items, optionally filtered by scope COMPLEX or COURT.'
  })
  @ApiQuery({
    name: 'scope',
    required: false,
    enum: SERVICE_SCOPE_VALUES,
    description: 'Optional scope filter for active service catalog items.'
  })
  @ApiEnvelopeArrayOk(
    ServiceCatalogResponse,
    'Active services wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401)
  async list(@Query() query: ListServicesQuery): Promise<ServiceCatalogResponse[]> {
    return this.listActiveServices.execute(query.scope);
  }
}
