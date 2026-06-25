import { Controller, Get, Inject, Param, ParseUUIDPipe, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiParam, ApiTags } from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import {
  ApiEnvelopeArrayOk,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { ListCantonsByProvinceUseCase } from '../../../application/use-cases/list-cantons-by-province.use-case';
import { ListProvincesUseCase } from '../../../application/use-cases/list-provinces.use-case';
import {
  CantonCatalogResponse,
  ProvinceCatalogResponse
} from '../dto/location-catalog.response';

/**
 * HTTP endpoints for controlled Costa Rica location catalogs.
 */
@ApiTags('locations')
@ApiBearerAuth()
@Controller('locations')
export class LocationsController {
  constructor(
    @Inject(ListProvincesUseCase)
    private readonly listProvinces: ListProvincesUseCase,
    @Inject(ListCantonsByProvinceUseCase)
    private readonly listCantonsByProvince: ListCantonsByProvinceUseCase
  ) {}

  @Get('provinces')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List controlled provinces for the complex wizard.',
    description:
      'Returns the active controlled province catalog available to authenticated wizard clients.'
  })
  @ApiEnvelopeArrayOk(
    ProvinceCatalogResponse,
    'Controlled provinces wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(401)
  async listProvincesCatalog(): Promise<ProvinceCatalogResponse[]> {
    return this.listProvinces.execute();
  }

  @Get('provinces/:provinceId/cantons')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List controlled cantons for one province.',
    description:
      'Returns only cantons that belong to the selected controlled province for the complex wizard.'
  })
  @ApiParam({
    name: 'provinceId',
    description: 'Controlled province identifier.',
    format: 'uuid'
  })
  @ApiEnvelopeArrayOk(
    CantonCatalogResponse,
    'Controlled cantons wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401)
  async listCantonsCatalog(
    @Param('provinceId', new ParseUUIDPipe()) provinceId: string
  ): Promise<CantonCatalogResponse[]> {
    return this.listCantonsByProvince.execute(provinceId);
  }
}
