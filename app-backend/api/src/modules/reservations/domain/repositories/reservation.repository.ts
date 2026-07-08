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

export interface IMyReservationsSnapshotGroups {
  upcoming: IMyReservationSnapshot[];
  finalized: IMyReservationSnapshot[];
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

  completeExpiredReservations(
    command: ICompleteExpiredReservationsCommand
  ): Promise<number>;
}

export const RESERVATION_REPOSITORY = Symbol('RESERVATION_REPOSITORY');
