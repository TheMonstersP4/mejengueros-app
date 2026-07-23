import { Controller, Get, Inject, Query } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import {
  ApiEnvelopeArrayOk,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { withApiMeta } from '@/shared/interfaces/http/responses/api-response';
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
      'Returns active and published catalog courts, optionally filtered by text, province, canton, service, and minimum rating. Results are paginated so clients can load the catalog incrementally.'
  })
  @ApiEnvelopeArrayOk(
    CourtCatalogResponse,
    'Public court catalog items wrapped in the API response envelope with pagination metadata.'
  )
  @ApiEnvelopeErrors(400, 502)
  async listCatalog(
    @Query() query: ListCourtCatalogQuery
  ): Promise<ReturnType<typeof withApiMeta<CourtCatalogResponse[]>>> {
    const result = await this.listPublicCourtCatalog.execute({
      q: query.q,
      provinceId: query.provinceId,
      cantonId: query.cantonId,
      serviceIds: query.serviceIds,
      minRating: query.minRating,
      page: query.page,
      pageSize: query.pageSize
    });

    const totalPages =
      result.pageSize > 0 ? Math.ceil(result.totalItems / result.pageSize) : 0;

    return withApiMeta(result.items as CourtCatalogResponse[], {
      pagination: {
        page: result.page,
        pageSize: result.pageSize,
        totalItems: result.totalItems,
        totalPages
      }
    });
  }
}
