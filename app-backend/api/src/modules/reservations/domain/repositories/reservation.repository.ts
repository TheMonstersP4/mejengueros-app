import type {
  ComplexStatus,
  CourtStatus,
  ReservationStatus,
  Weekday
} from '@/generated/prisma/enums';

export interface IReservationWindowQuery {
  courtId: string;
  date: string;
}

export interface IReservationWindowCourtSnapshot {
  id: string;
  name: string;
  status: CourtStatus;
  complexStatus: ComplexStatus;
}

export interface IReservationWindowAvailabilitySnapshot {
  days: Weekday[];
  startTime: string;
  endTime: string;
}

export interface IReservationWindowSnapshot {
  court: IReservationWindowCourtSnapshot;
  availability: IReservationWindowAvailabilitySnapshot | null;
  confirmedStartsAt: string[];
}

export interface ICreateConfirmedReservationCommand {
  userId: string;
  courtId: string;
  startsAt: Date;
  endsAt: Date;
}

export interface IReservationSnapshot {
  id: string;
  userId: string;
  courtId: string;
  startsAt: string;
  endsAt: string;
  status: ReservationStatus;
}

export interface IMyReservationSnapshot {
  id: string;
  complexName: string;
  courtName: string;
  imageObjectKey?: string;
  startsAt: string;
  endsAt: string;
  status: ReservationStatus;
  completedAt: string | null;
  reviewId: string | null;
}

export interface IFindMyReservationsQuery {
  userId: string;
  upcomingLimit: number;
  finalizedLimit: number;
}

export interface ICompleteExpiredReservationsCommand {
  now: Date;
}

export interface ICompletedReservationSnapshot {
  id: string;
  userId: string;
}

export interface IMyReservationsSnapshotGroups {
  upcoming: IMyReservationSnapshot[];
  finalized: IMyReservationSnapshot[];
}

/**
 * Authenticated owner identity used to scope owner-only reservation reads.
 */
export interface IOwnerReservationsIdentity {
  sub: string;
  provider?: string;
}

/**
 * Optional court filter applied to an owner reservations query.
 */
export interface IOwnerReservationCourtFilter {
  courtId: string;
}

/**
 * Query that powers the owner reservations endpoint. Lists reservations booked
 * by other players on courts the authenticated owner owns or administers.
 */
export interface IListOwnerReservationsQuery {
  ownerIdentity: IOwnerReservationsIdentity;
  court?: IOwnerReservationCourtFilter;
  upcomingLimit: number;
  finalizedLimit: number;
}

/**
 * A single owner-facing reservation row returned to the application layer.
 */
export interface IOwnerReservationSnapshot {
  id: string;
  complexName: string;
  courtName: string;
  imageObjectKey?: string;
  startsAt: string;
  endsAt: string;
  status: ReservationStatus;
}

export interface IOwnerReservationsSnapshotGroups {
  upcoming: IOwnerReservationSnapshot[];
  finalized: IOwnerReservationSnapshot[];
}

export interface IReservationRepository {
  getReservationWindow(
    query: IReservationWindowQuery
  ): Promise<IReservationWindowSnapshot>;

  createConfirmedReservation(
    command: ICreateConfirmedReservationCommand
  ): Promise<IReservationSnapshot>;

  findMyReservationsByUserId(
    query: IFindMyReservationsQuery
  ): Promise<IMyReservationsSnapshotGroups>;

  listOwnerReservations(
    query: IListOwnerReservationsQuery
  ): Promise<IOwnerReservationsSnapshotGroups>;

  completeExpiredReservations(
    command: ICompleteExpiredReservationsCommand
  ): Promise<ICompletedReservationSnapshot[]>;
}

export const RESERVATION_REPOSITORY = Symbol('RESERVATION_REPOSITORY');
