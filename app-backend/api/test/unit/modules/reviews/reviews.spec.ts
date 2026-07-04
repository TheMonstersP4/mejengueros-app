import type { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { ImageUploadEntity } from '@/modules/files/domain/entities/image-upload.entity';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import { CreateReviewUseCase } from '@/modules/reviews/application/use-cases/create-review.use-case';
import { GetLatestReviewableReservationUseCase } from '@/modules/reviews/application/use-cases/get-latest-reviewable-reservation.use-case';
import type {
  IReviewRepository,
  IReviewableReservationSnapshot,
  IReservationForReviewSnapshot
} from '@/modules/reviews/domain/repositories/review.repository';

describe('reviews module behavior', () => {
  const authenticatedUser = {
    sub: 'player-sub',
    email: 'player@example.test',
    emailVerified: true,
    name: 'Player One',
    pictureUrl: 'https://example.test/player.png',
    provider: 'Google',
    groups: ['players']
  };

  const completedReservation: IReservationForReviewSnapshot = {
    id: 'reservation-id',
    userId: 'user-id',
    status: 'COMPLETED',
    completedAt: '2026-07-03T01:00:00.000Z',
    reviewId: null
  };

  const latestReviewableReservation: IReviewableReservationSnapshot = {
    reservationId: 'reservation-id',
    complexName: 'Moravia FC',
    courtName: 'Cancha A',
    startsAt: '2026-07-02T20:00:00.000Z',
    endsAt: '2026-07-02T21:00:00.000Z',
    imageObjectKey: 'uploads/court-image/player-sub/2026/07/court-a.png'
  };

  function createSyncAuthenticatedUserUseCase(): SyncAuthenticatedUserUseCase {
    return {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
  }

  function createReviewRepository(): jest.Mocked<IReviewRepository> {
    return {
      findLatestReviewableReservationForUser: jest
        .fn()
        .mockResolvedValue(latestReviewableReservation),
      findReservationById: jest.fn().mockResolvedValue(completedReservation),
      findReviewIdByEvidenceImageUploadId: jest.fn().mockResolvedValue(null),
      createReview: jest.fn().mockResolvedValue({
        id: 'review-id',
        reservationId: 'reservation-id',
        rating: 1,
        comment: 'La iluminación falló toda la hora.',
        evidenceImageUploadId: 'evidence-image-id',
        createdAt: '2026-07-03T02:00:00.000Z'
      })
    };
  }

  function createImageUploadRepository(
    imageUpload: ImageUploadEntity | null =
      ImageUploadEntity.fromPersistence({
        id: 'evidence-image-id',
        ownerSub: 'player-sub',
        ownerEmail: 'player@example.test',
        ownerName: 'Player One',
        ownerPictureUrl: 'https://example.test/player.png',
        ownerProvider: 'Google',
        purpose: FilePurpose.ReviewEvidenceImage,
        objectKey: 'uploads/review-evidence-image/player-sub/2026/07/evidence.png',
        contentType: 'image/png',
        sizeBytes: 512,
        createdAt: new Date('2026-07-03T01:30:00.000Z')
      })
  ): jest.Mocked<IImageUploadRepository> {
    return {
      findById: jest.fn().mockResolvedValue(imageUpload),
      saveConfirmedUpload: jest.fn(),
      listRecent: jest.fn()
    };
  }

  it('creates a 1-star review when comment and evidence image are provided', async () => {
    const reviewRepository = createReviewRepository();
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 1,
        comment: 'La iluminación falló toda la hora.',
        evidenceImageUploadId: 'evidence-image-id'
      })
    ).resolves.toMatchObject({
      id: 'review-id',
      reservationId: 'reservation-id',
      rating: 1,
      evidenceImageUploadId: 'evidence-image-id'
    });
  });

  it('rejects a 1-star review without comment', async () => {
    const useCase = new CreateReviewUseCase(
      createReviewRepository(),
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 1,
        comment: '   ',
        evidenceImageUploadId: 'evidence-image-id'
      })
    ).rejects.toThrow('comment');
  });

  it('rejects a 1-star review without evidence image', async () => {
    const useCase = new CreateReviewUseCase(
      createReviewRepository(),
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 1,
        comment: 'La iluminación falló toda la hora.'
      })
    ).rejects.toThrow('evidence');
  });

  it('rejects evidence images with the wrong purpose', async () => {
    const useCase = new CreateReviewUseCase(
      createReviewRepository(),
      createImageUploadRepository(
        ImageUploadEntity.fromPersistence({
          id: 'evidence-image-id',
          ownerSub: 'player-sub',
          purpose: FilePurpose.CourtImage,
          objectKey: 'uploads/court-image/player-sub/2026/07/court.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-07-03T01:30:00.000Z')
        })
      ),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 1,
        comment: 'La iluminación falló toda la hora.',
        evidenceImageUploadId: 'evidence-image-id'
      })
    ).rejects.toThrow('purpose');
  });

  it('rejects evidence images owned by another user', async () => {
    const useCase = new CreateReviewUseCase(
      createReviewRepository(),
      createImageUploadRepository(
        ImageUploadEntity.fromPersistence({
          id: 'evidence-image-id',
          ownerSub: 'other-user-sub',
          purpose: FilePurpose.ReviewEvidenceImage,
          objectKey: 'uploads/review-evidence-image/other-user-sub/2026/07/evidence.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-07-03T01:30:00.000Z')
        })
      ),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 1,
        comment: 'La iluminación falló toda la hora.',
        evidenceImageUploadId: 'evidence-image-id'
      })
    ).rejects.toThrow('authenticated');
  });

  it('rejects evidence images already assigned to another review', async () => {
    const reviewRepository = createReviewRepository();
    reviewRepository.findReviewIdByEvidenceImageUploadId.mockResolvedValue('another-review-id');
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 1,
        comment: 'La iluminación falló toda la hora.',
        evidenceImageUploadId: 'evidence-image-id'
      })
    ).rejects.toThrow('already assigned');
  });

  it('rejects duplicate reviews for the same reservation', async () => {
    const reviewRepository = createReviewRepository();
    reviewRepository.findReservationById.mockResolvedValue({
      ...completedReservation,
      reviewId: 'existing-review-id'
    });
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 5
      })
    ).rejects.toThrow('already has a review');
  });

  it('rejects reservations that do not belong to the authenticated user', async () => {
    const reviewRepository = createReviewRepository();
    reviewRepository.findReservationById.mockResolvedValue({
      ...completedReservation,
      userId: 'other-user-id'
    });
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 5
      })
    ).rejects.toThrow('reservation');
  });

  it('rejects reservations that are not completed yet', async () => {
    const reviewRepository = createReviewRepository();
    reviewRepository.findReservationById.mockResolvedValue({
      ...completedReservation,
      status: 'CONFIRMED',
      completedAt: null
    });
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating: 5
      })
    ).rejects.toThrow('completed');
  });

  it.each([0, 6])(
    'rejects ratings outside the supported 1 to 5 range: %s',
    async (rating) => {
    const useCase = new CreateReviewUseCase(
      createReviewRepository(),
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(authenticatedUser, {
        reservationId: 'reservation-id',
        rating
      })
    ).rejects.toThrow('1 and 5');
    }
  );

  it('loads the latest eligible reviewable reservation for the authenticated user', async () => {
    const reviewRepository = createReviewRepository();
    const fileReadUrl = {
      createReadUrl: jest.fn().mockResolvedValue('https://read.example.test/court-a.png')
    };
    const useCase = new GetLatestReviewableReservationUseCase(
      reviewRepository,
      createSyncAuthenticatedUserUseCase(),
      fileReadUrl
    );

    await expect(useCase.execute(authenticatedUser)).resolves.toMatchObject({
      reservationId: 'reservation-id',
      complexName: 'Moravia FC',
      courtName: 'Cancha A',
      imageUrl: 'https://read.example.test/court-a.png'
    });
  });

  it('keeps the latest eligible reservation available when image URL enrichment fails', async () => {
    const reviewRepository = createReviewRepository();
    const fileReadUrl = {
      createReadUrl: jest.fn().mockRejectedValue(new Error('signed URL unavailable'))
    };
    const useCase = new GetLatestReviewableReservationUseCase(
      reviewRepository,
      createSyncAuthenticatedUserUseCase(),
      fileReadUrl
    );

    await expect(useCase.execute(authenticatedUser)).resolves.toEqual({
      reservationId: 'reservation-id',
      complexName: 'Moravia FC',
      courtName: 'Cancha A',
      startsAt: '2026-07-02T20:00:00.000Z',
      endsAt: '2026-07-02T21:00:00.000Z',
      imageUrl: undefined
    });
  });
});
