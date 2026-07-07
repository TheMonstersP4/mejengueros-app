import type { ReservationStatus } from '@/generated/prisma/enums';

/**
 * Authenticated owner identity used to scope owner-only review reads.
 */
export interface IReviewOwnerIdentity {
  sub: string;
  provider?: string;
}

/**
 * Optional court filter applied to an owner review query.
 */
export interface IReviewOwnerCourtFilter {
  courtId: string;
}

/**
 * Pagination window for owner review reads.
 */
export interface IReviewOwnerPagination {
  page: number;
  pageSize: number;
}

/**
 * Query that powers the owner reviews endpoint.
 */
export interface IListOwnerCourtReviewsQuery {
  ownerIdentity: IReviewOwnerIdentity;
  court?: IReviewOwnerCourtFilter;
  pagination: IReviewOwnerPagination;
}

/**
 * Reviewer display fields produced by the application layer.
 */
export interface IReviewerDisplay {
  displayName: string;
  initials: string;
}

/**
 * A single owner-review row returned to the HTTP boundary.
 */
export interface IOwnerCourtReviewItem {
  reviewId: string;
  rating: number;
  comment: string | null;
  createdAt: string;
  court: {
    id: string;
    name: string;
  };
  reviewer: IReviewerDisplay;
}

/**
 * Aggregate summary for the selected owner review scope.
 */
export interface IOwnerCourtReviewsSummary {
  selectedCourtId: string | null;
  totalReviews: number;
  averageRating: number | null;
}

/**
 * Final result returned by the owner reviews use case.
 */
export interface IListOwnerCourtReviewsResult {
  summary: IOwnerCourtReviewsSummary;
  items: IOwnerCourtReviewItem[];
  totalItems: number;
  page: number;
  pageSize: number;
}

/**
 * Pagination window for the public court reviews read.
 */
export interface IPublicCourtReviewsPagination {
  page: number;
  pageSize: number;
}

/**
 * Query that powers the public court reviews endpoint.
 */
export interface IListPublicCourtReviewsQuery {
  courtId: string;
  pagination: IPublicCourtReviewsPagination;
}

/**
 * Single public-review row returned to the HTTP boundary.
 */
export interface IPublicCourtReviewItem {
  reviewId: string;
  rating: number;
  comment: string | null;
  createdAt: string;
  reviewer: IReviewerDisplay;
}

/**
 * Aggregate summary for a court's public reviews.
 */
export interface IPublicCourtReviewsSummary {
  totalReviews: number;
  averageRating: number | null;
}

/**
 * Final result returned by the public court reviews use case.
 */
export interface IListPublicCourtReviewsResult {
  summary: IPublicCourtReviewsSummary;
  items: IPublicCourtReviewItem[];
  totalItems: number;
  page: number;
  pageSize: number;
}

export interface IReviewableReservationSnapshot {
  reservationId: string;
  complexName: string;
  courtName: string;
  startsAt: string;
  endsAt: string;
  imageObjectKey?: string;
}

export interface IReservationForReviewSnapshot {
  id: string;
  userId: string;
  status: ReservationStatus;
  completedAt?: string | null;
  reviewId?: string | null;
}

export interface ICreateReviewCommand {
  reservationId: string;
  rating: number;
  comment?: string;
  evidenceImageUploadId?: string;
}

export interface ICreatedReviewSnapshot {
  id: string;
  reservationId: string;
  rating: number;
  comment?: string;
  evidenceImageUploadId?: string;
  createdAt: string;
}

/**
 * Persistence contract for review reads and writes shared between the
 * owner reviews dashboard and the player review submission flows.
 */
export interface IReviewRepository {
  listOwnerCourtReviews(
    query: IListOwnerCourtReviewsQuery
  ): Promise<IListOwnerCourtReviewsResult>;

  /**
   * Returns the id of a court that is publicly visible (active, published,
   * not deleted, inside an active/published/non-deleted complex), or null
   * when the court does not exist or is not publicly visible.
   */
  findPubliclyVisibleCourtId(courtId: string): Promise<string | null>;

  listPublicCourtReviews(
    query: IListPublicCourtReviewsQuery
  ): Promise<IListPublicCourtReviewsResult>;

  findLatestReviewableReservationForUser(
    userId: string
  ): Promise<IReviewableReservationSnapshot | null>;

  findReservationById(
    reservationId: string
  ): Promise<IReservationForReviewSnapshot | null>;

  findReviewIdByEvidenceImageUploadId(
    evidenceImageUploadId: string
  ): Promise<string | null>;

  createReview(command: ICreateReviewCommand): Promise<ICreatedReviewSnapshot>;
}

/**
 * Dependency injection token for the review repository port.
 */
export const REVIEW_REPOSITORY = Symbol('REVIEW_REPOSITORY');
