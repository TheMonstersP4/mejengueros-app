export interface ICourtAdminSnapshot {
  id: string;
  name: string;
  status: string;
  updatedAt: string;
}

export interface ICourtAdminRepository {
  deactivateById(courtId: string): Promise<ICourtAdminSnapshot>;
  reactivateById(courtId: string): Promise<ICourtAdminSnapshot>;
}

export const COURT_ADMIN_REPOSITORY = Symbol('COURT_ADMIN_REPOSITORY');
