export type ReservationCardStatus = 'CONFIRMED' | 'COMPLETED';

export type ReservationCardSection = 'UPCOMING' | 'FINALIZED';

export type ReservationCardReviewStatus =
  | 'NOT_APPLICABLE'
  | 'PENDING_REVIEW'
  | 'REVIEWED';

/**
 * Render-ready reservation card returned by the My Reservations query.
 */
export interface IReservationCardOutput {
  id: string;
  complexName: string;
  courtName: string;
  imageUrl?: string;
  startsAt: string;
  endsAt: string;
  status: ReservationCardStatus;
  section: ReservationCardSection;
  reviewStatus: ReservationCardReviewStatus;
  canReview: boolean;
  hasReview: boolean;
  primaryActionKey?: 'leave_review';
  primaryActionLabel?: 'Dejar reseña';
  indicatorKey?: 'already_reviewed';
  indicatorLabel?: 'Ya dejaste tu reseña';
}

/**
 * Grouped My Reservations payload consumed by the authenticated player screen.
 */
export interface IMyReservationsOutput {
  upcoming: IReservationCardOutput[];
  finalized: IReservationCardOutput[];
}
