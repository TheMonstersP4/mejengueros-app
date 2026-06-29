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

export interface IReservationRepository {
  getReservationWindow(
    query: IReservationWindowQuery
  ): Promise<IReservationWindowSnapshot>;

  createConfirmedReservation(
    command: ICreateConfirmedReservationCommand
  ): Promise<IReservationSnapshot>;
}

export const RESERVATION_REPOSITORY = Symbol('RESERVATION_REPOSITORY');
