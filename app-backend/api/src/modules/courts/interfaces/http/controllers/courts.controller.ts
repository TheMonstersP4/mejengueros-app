import { Controller, Get, Inject, Param, ParseUUIDPipe, Patch, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiParam, ApiTags } from '@nestjs/swagger';
import {
  ApiEnvelopeArrayOk,
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { withApiMeta } from '@/shared/interfaces/http/responses/api-response';
import type { IAuthenticatedUserOutput } from '@/modules/auth/application/dto/authenticated-user.output';
import { CognitoAuthGuard } from '@/modules/auth/interfaces/http/guards/cognito-auth.guard';
import { CurrentUser } from '@/shared/interfaces/http/decorators/current-user.decorator';
import { DeactivateCourtUseCase } from '../../../application/use-cases/deactivate-court.use-case';
import { ListPublicCourtCatalogUseCase } from '../../../application/use-cases/list-public-court-catalog.use-case';
import { CourtCatalogResponse } from '../dto/court-catalog.response';
import { DeactivateCourtResponse } from '../dto/deactivate-court.response';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { ListCourtCatalogQuery } from '../dto/list-court-catalog.query';

@ApiTags('courts')
@Controller('courts')
export class CourtsController {
  constructor(
    @Inject(ListPublicCourtCatalogUseCase)
    private readonly listPublicCourtCatalog: ListPublicCourtCatalogUseCase,
    @Inject(DeactivateCourtUseCase)
    private readonly deactivateCourt: DeactivateCourtUseCase
  ) {}

  @Get('catalog')
  @ApiOperation({
    summary: 'List the public court catalog.',
    description:
      'Returns active and published catalog courts, optionally filtered by text, province, canton, service, court identifiers, and minimum rating. Results are paginated so clients can load the catalog incrementally.'
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
      courtIds: query.courtIds,
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

  @Patch(':courtId/deactivate')
  @UseGuards(CognitoAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({
    summary: 'Deactivate one court.',
    description:
      'Requires an authenticated administrator and marks the court inactive without deleting historical reservations or related records.'
  })
  @ApiParam({ name: 'courtId', description: 'Court identifier.', format: 'uuid' })
  @ApiEnvelopeOk(
    DeactivateCourtResponse,
    'Deactivated court wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 403, 404)
  async deactivate(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Param('courtId', new ParseUUIDPipe()) courtId: string
  ): Promise<DeactivateCourtResponse> {
    return {
      court: await this.deactivateCourt.execute(user, courtId)
    };
  }
}
