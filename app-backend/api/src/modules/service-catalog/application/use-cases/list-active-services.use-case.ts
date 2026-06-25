import { Inject, Injectable } from '@nestjs/common';
import type {
  IServiceCatalogItem,
  IServiceCatalogRepository,
  IServiceCatalogScope
} from '../../domain/repositories/service-catalog.repository';
import { SERVICE_CATALOG_REPOSITORY } from '../../domain/repositories/service-catalog.repository';

/**
 * Returns active service catalog items for complex or court wizard steps.
 */
@Injectable()
export class ListActiveServicesUseCase {
  constructor(
    @Inject(SERVICE_CATALOG_REPOSITORY)
    private readonly serviceCatalogRepository: IServiceCatalogRepository
  ) {}

  execute(scope?: IServiceCatalogScope): Promise<IServiceCatalogItem[]> {
    return this.serviceCatalogRepository.listActiveServices(scope);
  }
}
