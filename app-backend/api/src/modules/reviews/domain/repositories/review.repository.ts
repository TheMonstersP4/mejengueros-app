import type { ReservationStatus } from '@/generated/prisma/enums';

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

export interface IReviewRepository {
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

export const REVIEW_REPOSITORY = Symbol('REVIEW_REPOSITORY');
