import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';
import { InvalidReviewEvidenceUploadError } from '../../domain/errors/invalid-review-evidence-upload.error';
import { ReviewAlreadyExistsError } from '../../domain/errors/review-already-exists.error';
import type {
  ICreatedReviewSnapshot,
  ICreateReviewCommand,
  IReservationForReviewSnapshot,
  IReviewRepository,
  IReviewableReservationSnapshot
} from '../../domain/repositories/review.repository';

interface IReviewPersistenceClient {
  reservation: PrismaService['reservation'];
  review: PrismaService['review'];
}

@Injectable()
export class PrismaReviewRepository implements IReviewRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: IReviewPersistenceClient
  ) {}

  async findLatestReviewableReservationForUser(
    userId: string
  ): Promise<IReviewableReservationSnapshot | null> {
    const reservation = await this.prisma.reservation.findFirst({
      where: {
        userId,
        status: 'COMPLETED',
        completedAt: { not: null },
        review: null,
        court: {
          deletedAt: null,
          complex: { deletedAt: null }
        }
      },
      orderBy: [{ completedAt: 'desc' }, { startsAt: 'desc' }],
      select: {
        id: true,
        startsAt: true,
        endsAt: true,
        court: {
          select: {
            name: true,
            imageUpload: {
              select: { objectKey: true }
            },
            complex: {
              select: { name: true }
            }
          }
        }
      }
    });

    if (reservation == null) {
      return null;
    }

    return {
      reservationId: reservation.id,
      complexName: reservation.court.complex.name,
      courtName: reservation.court.name,
      startsAt: reservation.startsAt.toISOString(),
      endsAt: reservation.endsAt.toISOString(),
      imageObjectKey: reservation.court.imageUpload?.objectKey ?? undefined
    };
  }

  async findReservationById(
    reservationId: string
  ): Promise<IReservationForReviewSnapshot | null> {
    const reservation = await this.prisma.reservation.findUnique({
      where: { id: reservationId },
      select: {
        id: true,
        userId: true,
        status: true,
        completedAt: true,
        review: {
          select: { id: true }
        }
      }
    });

    if (reservation == null) {
      return null;
    }

    return {
      id: reservation.id,
      userId: reservation.userId,
      status: reservation.status,
      completedAt: reservation.completedAt?.toISOString() ?? null,
      reviewId: reservation.review?.id ?? null
    };
  }

  async findReviewIdByEvidenceImageUploadId(
    evidenceImageUploadId: string
  ): Promise<string | null> {
    const review = await this.prisma.review.findFirst({
      where: { evidenceImageUploadId },
      select: { id: true }
    });

    return review?.id ?? null;
  }

  async createReview(command: ICreateReviewCommand): Promise<ICreatedReviewSnapshot> {
    try {
      const review = await this.prisma.review.create({
        data: {
          reservationId: command.reservationId,
          rating: command.rating,
          comment: command.comment,
          evidenceImageUploadId: command.evidenceImageUploadId
        }
      });

      return {
        id: review.id,
        reservationId: review.reservationId,
        rating: review.rating,
        comment: review.comment ?? undefined,
        evidenceImageUploadId: review.evidenceImageUploadId ?? undefined,
        createdAt: review.createdAt.toISOString()
      };
    } catch (error) {
      if (this.isUniqueConstraintOn(error, 'reservationId')) {
        throw new ReviewAlreadyExistsError(command.reservationId);
      }

      if (this.isUniqueConstraintOn(error, 'evidenceImageUploadId')) {
        throw InvalidReviewEvidenceUploadError.alreadyAssigned(
          command.evidenceImageUploadId ?? ''
        );
      }

      throw error;
    }
  }

  private isUniqueConstraintOn(error: unknown, field: string): boolean {
    if (
      typeof error !== 'object' ||
      error == null ||
      !('code' in error) ||
      error.code !== 'P2002' ||
      !('meta' in error)
    ) {
      return false;
    }

    const meta = error.meta as { target?: string | string[] } | undefined;
    const targets = Array.isArray(meta?.target)
      ? meta.target
      : meta?.target == null
        ? []
        : [meta.target];

    return targets.some((target) => String(target).includes(field));
  }
}
