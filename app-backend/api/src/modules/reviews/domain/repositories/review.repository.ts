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
 * Persistence contract for owner-only review reads.
 */
export interface IReviewRepository {
  listOwnerCourtReviews(
    query: IListOwnerCourtReviewsQuery
  ): Promise<IListOwnerCourtReviewsResult>;
}

/**
 * Dependency injection token for the review repository port.
 */
export const REVIEW_REPOSITORY = Symbol('REVIEW_REPOSITORY');
