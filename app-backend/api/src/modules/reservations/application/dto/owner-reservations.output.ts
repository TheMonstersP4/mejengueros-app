export type OwnerReservationCardStatus = 'CONFIRMED' | 'COMPLETED';

export type OwnerReservationCardSection = 'UPCOMING' | 'FINALIZED';

/**
 * Render-ready owner-facing reservation card returned by the Owner Reservations
 * query. Mirrors the My Reservations layout without the review call-to-action.
 */
export interface IOwnerReservationCardOutput {
  id: string;
  complexName: string;
  courtName: string;
  imageUrl?: string;
  startsAt: string;
  endsAt: string;
  status: OwnerReservationCardStatus;
  section: OwnerReservationCardSection;
}

/**
 * Grouped Owner Reservations payload consumed by the owner-facing screen. The
 * `selectedCourtId` echoes the applied court filter, or null when listing every
 * owned court.
 */
export interface IOwnerReservationsOutput {
  selectedCourtId: string | null;
  upcoming: IOwnerReservationCardOutput[];
  finalized: IOwnerReservationCardOutput[];
}
