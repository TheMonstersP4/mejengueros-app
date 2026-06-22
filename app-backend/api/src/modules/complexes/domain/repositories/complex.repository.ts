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
  address: string;
}

/**
 * Required first-court fields for the first creation flow.
 */
export interface ICreateFirstCourtInput {
  name: string;
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
  address: string;
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

/**
 * Persistence contract for complex creation flows.
 */
export interface IComplexRepository {
  createComplexWithFirstCourt(
    command: ICreateComplexWithFirstCourtCommand
  ): Promise<ICreateComplexWithFirstCourtResult>;
}

/**
 * Dependency injection token for the complex repository port.
 */
export const COMPLEX_REPOSITORY = Symbol('COMPLEX_REPOSITORY');
