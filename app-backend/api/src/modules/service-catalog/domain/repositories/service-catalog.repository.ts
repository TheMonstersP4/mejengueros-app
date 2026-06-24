/**
 * Scope supported by the wizard service catalog endpoint.
 */
export type IServiceCatalogScope = 'COMPLEX' | 'COURT';

/**
 * Active service exposed to wizard clients.
 */
export interface IServiceCatalogItem {
  id: string;
  name: string;
  scope: IServiceCatalogScope;
}

/**
 * Read-only active service catalog contract.
 */
export interface IServiceCatalogRepository {
  listActiveServices(scope?: IServiceCatalogScope): Promise<IServiceCatalogItem[]>;
}

/**
 * Dependency injection token for active service catalog queries.
 */
export const SERVICE_CATALOG_REPOSITORY = Symbol('SERVICE_CATALOG_REPOSITORY');
