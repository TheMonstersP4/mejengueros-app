export interface ICourtCatalogPagination {
  page: number;
  pageSize: number;
}

export interface ICourtCatalogFilters {
  q?: string;
  provinceId?: string;
  cantonId?: string;
  pagination: ICourtCatalogPagination;
}

export interface ICourtCatalogLocationItem {
  id: string;
  name: string;
}

export interface ICourtCatalogRatingItem {
  average: number | null;
  count: number;
}

export interface ICourtCatalogItem {
  courtId: string;
  courtName: string;
  complexId: string;
  complexName: string;
  province: ICourtCatalogLocationItem;
  canton: ICourtCatalogLocationItem;
  services: string[];
  rating: ICourtCatalogRatingItem;
  isReservableToday: boolean;
  imageUrl: string | null;
}

export interface ICourtCatalogPage {
  items: ICourtCatalogItem[];
  totalItems: number;
  page: number;
  pageSize: number;
}

export interface ICourtCatalogRepository {
  assertProvinceAndCantonMatch(provinceId: string, cantonId: string): Promise<void>;
  listPublicCatalog(filters: ICourtCatalogFilters): Promise<ICourtCatalogPage>;
}

export const COURT_CATALOG_REPOSITORY = Symbol('COURT_CATALOG_REPOSITORY');
