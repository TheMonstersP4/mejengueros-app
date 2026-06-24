/**
 * Province catalog item exposed to the application layer.
 */
export interface IProvinceCatalogItem {
  id: string;
  code: string;
  name: string;
}

/**
 * Canton catalog item exposed to the application layer.
 */
export interface ICantonCatalogItem {
  id: string;
  provinceId: string;
  code: string;
  name: string;
}

/**
 * Read-only controlled location catalog contract.
 */
export interface ILocationCatalogRepository {
  listProvinces(): Promise<IProvinceCatalogItem[]>;
  listCantonsByProvince(provinceId: string): Promise<ICantonCatalogItem[]>;
}

/**
 * Dependency injection token for controlled location catalogs.
 */
export const LOCATION_CATALOG_REPOSITORY = Symbol('LOCATION_CATALOG_REPOSITORY');
