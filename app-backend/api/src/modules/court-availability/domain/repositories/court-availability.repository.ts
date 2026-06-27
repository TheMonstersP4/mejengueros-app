import type { Weekday } from '@/generated/prisma/enums';

export interface IAvailabilityOwnerIdentity {
  sub: string;
  provider?: string;
}

export interface ICourtAvailabilityInput {
  days: Weekday[];
  startTime: string;
  endTime: string;
}

export interface ICourtSummarySnapshot {
  id: string;
  name: string;
  complexId: string;
  complexName: string;
}

export interface ICourtAvailabilitySnapshot {
  days: Weekday[];
  startTime: string;
  endTime: string;
}

export interface ICourtAvailabilityState {
  court: ICourtSummarySnapshot;
  availability: ICourtAvailabilitySnapshot | null;
}

export interface IGetOwnedCourtAvailabilityQuery {
  ownerIdentity: IAvailabilityOwnerIdentity;
  courtId: string;
}

export interface ISaveOwnedCourtAvailabilityCommand extends IGetOwnedCourtAvailabilityQuery {
  availability: ICourtAvailabilityInput;
}

export interface ICourtAvailabilityRepository {
  getOwnedCourtAvailability(
    query: IGetOwnedCourtAvailabilityQuery
  ): Promise<ICourtAvailabilityState>;

  saveOwnedCourtAvailability(
    command: ISaveOwnedCourtAvailabilityCommand
  ): Promise<ICourtAvailabilityState>;
}

export const COURT_AVAILABILITY_REPOSITORY = Symbol('COURT_AVAILABILITY_REPOSITORY');
