import { Controller, Get, Inject, Query } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import {
  ApiEnvelopeArrayOk,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { ListPublicCourtCatalogUseCase } from '../../../application/use-cases/list-public-court-catalog.use-case';
import { CourtCatalogResponse } from '../dto/court-catalog.response';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { ListCourtCatalogQuery } from '../dto/list-court-catalog.query';

@ApiTags('courts')
@Controller('courts')
export class CourtsController {
  constructor(
    @Inject(ListPublicCourtCatalogUseCase)
    private readonly listPublicCourtCatalog: ListPublicCourtCatalogUseCase
  ) {}

  @Get('catalog')
  @ApiOperation({
    summary: 'List the public court catalog.',
    description:
      'Returns active and published catalog courts, optionally filtered by text, province, and canton.'
  })
  @ApiEnvelopeArrayOk(
    CourtCatalogResponse,
    'Public court catalog items wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 502)
  async listCatalog(
    @Query() query: ListCourtCatalogQuery
  ): Promise<CourtCatalogResponse[]> {
    return this.listPublicCourtCatalog.execute(query);
  }
}
