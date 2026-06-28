/**
 * Authenticated owner identity required to create a complex.
 */
export interface IComplexOwnerIdentity {
  sub: string;
  email?: string;
  emailVerified?: boolean;
  name?: string;
  pictureUrl?: string;
  provider?: string;
}

/**
 * Required complex fields for the first creation flow.
 */
export interface ICreateComplexInput {
  name: string;
  provinceId: string;
  cantonId: string;
  address: string;
  latitude?: number;
  longitude?: number;
  serviceIds: string[];
}

/**
 * Required first-court fields for the first creation flow.
 */
export interface ICreateFirstCourtInput {
  name: string;
  serviceIds: string[];
}

/**
 * Repository command for atomic complex creation.
 */
export interface ICreateComplexWithFirstCourtCommand {
  ownerIdentity: IComplexOwnerIdentity;
  complex: ICreateComplexInput;
  firstCourt: ICreateFirstCourtInput;
}

/**
 * Created complex snapshot returned to the application layer.
 */
export interface ICreatedComplexSnapshot {
  id: string;
  name: string;
  provinceId: string;
  cantonId: string;
  address: string;
  latitude?: number;
  longitude?: number;
  serviceIds: string[];
  status: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Created first-court snapshot returned to the application layer.
 */
export interface ICreatedCourtSnapshot {
  id: string;
  complexId: string;
  name: string;
  serviceIds: string[];
  status: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Result returned after the atomic creation flow succeeds.
 */
export interface ICreateComplexWithFirstCourtResult {
  complex: ICreatedComplexSnapshot;
  firstCourt: ICreatedCourtSnapshot;
}

export interface IMyComplexHubOwnerIdentity {
  sub: string;
  provider?: string;
}

export interface IGetMyComplexHubQuery {
  ownerIdentity: IMyComplexHubOwnerIdentity;
}

export type IMyComplexCourtAvailabilityStatus = 'CONFIGURED' | 'PENDING';

export interface IMyComplexHubCourtSnapshot {
  id: string;
  name: string;
  status: string;
  availabilityStatus: IMyComplexCourtAvailabilityStatus;
}

export interface IMyComplexHubComplexSnapshot {
  id: string;
  name: string;
  address: string;
  provinceId?: string;
  cantonId?: string;
  latitude?: number;
  longitude?: number;
  status: string;
  courts: IMyComplexHubCourtSnapshot[];
}

export interface IGetMyComplexHubResult {
  complexes: IMyComplexHubComplexSnapshot[];
}

/**
 * Persistence contract for complex creation flows.
 */
export interface IComplexRepository {
  createComplexWithFirstCourt(
    command: ICreateComplexWithFirstCourtCommand
  ): Promise<ICreateComplexWithFirstCourtResult>;

  getMyComplexHub(query: IGetMyComplexHubQuery): Promise<IGetMyComplexHubResult>;
}

/**
 * Dependency injection token for the complex repository port.
 */
export const COMPLEX_REPOSITORY = Symbol('COMPLEX_REPOSITORY');
