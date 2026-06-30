export interface ICourtCatalogFilters {
  q?: string;
  provinceId?: string;
  cantonId?: string;
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

export interface ICourtCatalogRepository {
  assertProvinceAndCantonMatch(provinceId: string, cantonId: string): Promise<void>;
  listPublicCatalog(filters: ICourtCatalogFilters): Promise<ICourtCatalogItem[]>;
}

export const COURT_CATALOG_REPOSITORY = Symbol('COURT_CATALOG_REPOSITORY');
