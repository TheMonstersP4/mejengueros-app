import type { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { ImageUploadEntity } from '@/modules/files/domain/entities/image-upload.entity';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import {
  ListOwnerCourtReviewsUseCase,
  OWNER_REVIEWS_DEFAULT_PAGE_SIZE,
  OWNER_REVIEWS_MAX_PAGE,
  OWNER_REVIEWS_MAX_PAGE_SIZE
} from '@/modules/reviews/application/use-cases/list-owner-court-reviews.use-case';
import {
  buildReviewerDisplayName,
  buildReviewerInitials
} from '@/modules/reviews/application/services/format-reviewer-identity';
import { CreateReviewUseCase } from '@/modules/reviews/application/use-cases/create-review.use-case';
import { GetLatestReviewableReservationUseCase } from '@/modules/reviews/application/use-cases/get-latest-reviewable-reservation.use-case';
import {
  ListPublicCourtReviewsUseCase,
  PUBLIC_COURT_REVIEWS_DEFAULT_PAGE_SIZE,
  PUBLIC_COURT_REVIEWS_MAX_PAGE,
  PUBLIC_COURT_REVIEWS_MAX_PAGE_SIZE
} from '@/modules/reviews/application/use-cases/list-public-court-reviews.use-case';
import { OwnerReviewCourtNotAccessibleError } from '@/modules/reviews/domain/errors/owner-review-court-not-accessible.error';
import { PublicCourtReviewsCourtNotFoundError } from '@/modules/reviews/domain/errors/public-court-not-found.error';
import type {
  IListOwnerCourtReviewsQuery,
  IListOwnerCourtReviewsResult,
  IListPublicCourtReviewsResult,
  IReviewRepository,
  IReviewableReservationSnapshot,
  IReservationForReviewSnapshot
} from '@/modules/reviews/domain/repositories/review.repository';
import { PrismaReviewRepository } from '@/modules/reviews/infrastructure/persistence/prisma-review.repository';
import { OwnerReviewsController } from '@/modules/reviews/interfaces/http/controllers/owner-reviews.controller';
import { PublicCourtReviewsController } from '@/modules/reviews/interfaces/http/controllers/public-court-reviews.controller';
import { ListOwnerCourtReviewsQuery } from '@/modules/reviews/interfaces/http/dto/list-owner-court-reviews.query';
import { ListPublicCourtReviewsQuery } from '@/modules/reviews/interfaces/http/dto/list-public-court-reviews.query';
import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';

describe('reviews module behavior', () => {
  const authenticatedUser = {
    sub: 'owner-sub',
    email: 'owner@example.test',
    emailVerified: true,
    name: 'Owner User',
    pictureUrl: 'https://example.test/owner.png',
    provider: 'Google',
    groups: ['owners']
  };

  const pageResult: IListOwnerCourtReviewsResult = {
    summary: {
      selectedCourtId: null,
      totalReviews: 1,
      averageRating: 4.5
    },
    items: [
      {
        reviewId: 'review-id',
        rating: 5,
        comment: 'Great court',
        createdAt: '2026-07-01T18:00:00.000Z',
        court: { id: 'court-id', name: 'Court A' },
        reviewer: { displayName: 'Diego R.', initials: 'DR' }
      }
    ],
    totalItems: 1,
    page: 1,
    pageSize: 10
  };

  it('formats display names with first name plus last initial', () => {
    expect(buildReviewerDisplayName('Diego Rivera')).toBe('Diego R.');
    expect(buildReviewerDisplayName('María Solís Vargas')).toBe('María V.');
  });

  it('falls back to "Player" when the reviewer has no stored name', () => {
    expect(buildReviewerDisplayName(null)).toBe('Player');
    expect(buildReviewerDisplayName('')).toBe('Player');
    expect(buildReviewerDisplayName('   ')).toBe('Player');
  });

  it('uses just the first name when there is no surname', () => {
    expect(buildReviewerDisplayName('Diego')).toBe('Diego.');
  });

  it('builds two-letter uppercase initials from the stored name', () => {
    expect(buildReviewerInitials('Diego Rivera')).toBe('DR');
    expect(buildReviewerInitials('María Solís Vargas')).toBe('MV');
  });

  it('pads single-name initials to two characters', () => {
    expect(buildReviewerInitials('Diego')).toBe('DD');
  });

  it('falls back to a stable "PP" initial when the reviewer has no stored name', () => {
    expect(buildReviewerInitials(null)).toBe('PP');
    expect(buildReviewerInitials('')).toBe('PP');
    expect(buildReviewerInitials('   ')).toBe('PP');
  });

  it('forwards the owner reviews query through the repository port', async () => {
    const repository = {
      listOwnerCourtReviews: jest.fn().mockResolvedValue(pageResult)
    } satisfies Partial<IReviewRepository>;
    const useCase = new ListOwnerCourtReviewsUseCase(
      repository as unknown as IReviewRepository
    );

    await expect(
      useCase.execute(authenticatedUser, {
        page: 2,
        pageSize: 5,
        courtId: 'court-id'
      })
    ).resolves.toEqual(pageResult);

    expect(repository.listOwnerCourtReviews).toHaveBeenCalledWith({
      ownerIdentity: {
        sub: 'owner-sub',
        provider: 'Google'
      },
      court: { courtId: 'court-id' },
      pagination: { page: 2, pageSize: 5 }
    });
  });

  it('omits the court filter from the repository query when not provided', async () => {
    const repository = {
      listOwnerCourtReviews: jest.fn().mockResolvedValue({
        ...pageResult,
        items: [],
        totalItems: 0,
        summary: { selectedCourtId: null, totalReviews: 0, averageRating: null }
      })
    } satisfies Partial<IReviewRepository>;
    const useCase = new ListOwnerCourtReviewsUseCase(
      repository as unknown as IReviewRepository
    );

    await useCase.execute(authenticatedUser, {
      page: 1,
      pageSize: OWNER_REVIEWS_DEFAULT_PAGE_SIZE
    });

    const callArg = repository.listOwnerCourtReviews.mock.calls[0]?.[0] as
      | IListOwnerCourtReviewsQuery
      | undefined;
    expect(callArg).toBeDefined();
    expect(callArg).not.toHaveProperty('court');
    expect(callArg?.pagination).toEqual({ page: 1, pageSize: OWNER_REVIEWS_DEFAULT_PAGE_SIZE });
  });

  it('builds the owner reviews HTTP response with pagination metadata', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({
        ...pageResult,
        totalItems: 25,
        page: 2,
        pageSize: 10
      })
    } as unknown as ListOwnerCourtReviewsUseCase;
    const controller = new OwnerReviewsController(useCase);

    const response = await controller.list(authenticatedUser, {
      page: 2,
      pageSize: 10
    });

    expect(response.data).toEqual({
      summary: pageResult.summary,
      items: pageResult.items
    });
    expect(response.meta?.pagination).toEqual({
      page: 2,
      pageSize: 10,
      totalItems: 25,
      totalPages: 3
    });
    expect(useCase.execute).toHaveBeenCalledWith(authenticatedUser, {
      page: 2,
      pageSize: 10,
      courtId: undefined
    });
  });

  it('renders an empty owner reviews response with zero totals and null average', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({
        summary: { selectedCourtId: null, totalReviews: 0, averageRating: null },
        items: [],
        totalItems: 0,
        page: 1,
        pageSize: 10
      })
    } as unknown as ListOwnerCourtReviewsUseCase;
    const controller = new OwnerReviewsController(useCase);

    const response = await controller.list(authenticatedUser, {
      page: 1,
      pageSize: 10
    });

    expect(response.data).toEqual({
      summary: { selectedCourtId: null, totalReviews: 0, averageRating: null },
      items: []
    });
    expect(response.meta?.pagination).toEqual({
      page: 1,
      pageSize: 10,
      totalItems: 0,
      totalPages: 0
    });
  });

  it('treats the per-court summary scope as the selected court identifier', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({
        summary: { selectedCourtId: 'court-id', totalReviews: 3, averageRating: 5 },
        items: [],
        totalItems: 3,
        page: 1,
        pageSize: 10
      })
    } as unknown as ListOwnerCourtReviewsUseCase;
    const controller = new OwnerReviewsController(useCase);

    const response = await controller.list(authenticatedUser, {
      page: 1,
      pageSize: 10,
      courtId: 'court-id'
    });

    expect(response.data.summary.selectedCourtId).toBe('court-id');
  });

  it('caps the page size at 50 and defaults to 10', () => {
    expect(OWNER_REVIEWS_MAX_PAGE_SIZE).toBe(50);
    expect(OWNER_REVIEWS_DEFAULT_PAGE_SIZE).toBe(10);
  });

  it('caps the page number at 10000 to prevent huge offset abuse', async () => {
    expect(OWNER_REVIEWS_MAX_PAGE).toBe(10_000);

    const oversized = plainToInstance(ListOwnerCourtReviewsQuery, {
      page: OWNER_REVIEWS_MAX_PAGE + 1,
      pageSize: 10
    });
    const errors = await validate(oversized);
    expect(errors).toHaveLength(1);
    expect(errors[0]?.property).toBe('page');
    expect(errors[0]?.constraints?.max).toBeDefined();

    const atLimit = plainToInstance(ListOwnerCourtReviewsQuery, {
      page: OWNER_REVIEWS_MAX_PAGE,
      pageSize: 10
    });
    await expect(validate(atLimit)).resolves.toEqual([]);
  });

  it('returns an empty result when the owner has no courts', async () => {
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue([])
      },
      review: {
        findMany: jest.fn(),
        count: jest.fn()
      },
      $queryRaw: jest.fn()
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await expect(
      repository.listOwnerCourtReviews({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        pagination: { page: 1, pageSize: 10 }
      })
    ).resolves.toEqual({
      summary: { selectedCourtId: null, totalReviews: 0, averageRating: null },
      items: [],
      totalItems: 0,
      page: 1,
      pageSize: 10
    });

    expect(prisma.review.findMany).not.toHaveBeenCalled();
    expect(prisma.review.count).not.toHaveBeenCalled();
    expect(prisma.$queryRaw).not.toHaveBeenCalled();
  });

  it('returns reviews for every owned court when no court filter is provided', async () => {
    const ownedCourts = [{ id: 'court-a' }, { id: 'court-b' }];
    const reviewRows = [
      {
        id: 'review-a',
        rating: 4,
        comment: 'Good court',
        createdAt: new Date('2026-07-02T18:00:00.000Z'),
        reservation: {
          court: { id: 'court-a', name: 'Court A' },
          user: { name: 'Diego Rivera', email: 'diego@example.test' }
        }
      },
      {
        id: 'review-b',
        rating: 5,
        comment: null,
        createdAt: new Date('2026-07-01T18:00:00.000Z'),
        reservation: {
          court: { id: 'court-b', name: 'Court B' },
          user: { name: 'María Solís', email: 'maria@example.test' }
        }
      }
    ];
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue(ownedCourts)
      },
      review: {
        findMany: jest.fn().mockResolvedValue(reviewRows),
        count: jest.fn().mockResolvedValue(2)
      },
      $queryRaw: jest
        .fn()
        .mockResolvedValue([{ average: 4.5 }])
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await expect(
      repository.listOwnerCourtReviews({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        pagination: { page: 1, pageSize: 10 }
      })
    ).resolves.toEqual({
      summary: {
        selectedCourtId: null,
        totalReviews: 2,
        averageRating: 4.5
      },
      items: [
        {
          reviewId: 'review-a',
          rating: 4,
          comment: 'Good court',
          createdAt: '2026-07-02T18:00:00.000Z',
          court: { id: 'court-a', name: 'Court A' },
          reviewer: { displayName: 'Diego R.', initials: 'DR' }
        },
        {
          reviewId: 'review-b',
          rating: 5,
          comment: null,
          createdAt: '2026-07-01T18:00:00.000Z',
          court: { id: 'court-b', name: 'Court B' },
          reviewer: { displayName: 'María S.', initials: 'MS' }
        }
      ],
      totalItems: 2,
      page: 1,
      pageSize: 10
    });

    expect(prisma.court.findMany).toHaveBeenCalledWith({
      where: {
        deletedAt: null,
        complex: {
          deletedAt: null,
          owner: {
            identities: {
              some: {
                provider: 'Google',
                providerSubject: 'owner-sub'
              }
            }
          }
        }
      },
      select: { id: true }
    });
    expect(prisma.review.findMany).toHaveBeenCalledWith({
      where: { reservation: { courtId: { in: ['court-a', 'court-b'] } } },
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      skip: 0,
      take: 10,
      select: expect.objectContaining({
        reservation: {
          select: {
            court: { select: { id: true, name: true } },
            user: { select: { name: true } }
          }
        }
      })
    });
    expect(prisma.review.findMany.mock.calls[0]?.[0].select).not.toHaveProperty(
      'reservation.pictureUrl'
    );
    // Privacy guard: the repository must not pull reviewer PII that is not
    // surfaced in the response.
    expect(
      prisma.review.findMany.mock.calls[0]?.[0].select?.reservation?.select?.user
    ).not.toHaveProperty('email');
    expect(
      prisma.review.findMany.mock.calls[0]?.[0].select?.reservation?.select?.user
    ).not.toHaveProperty('pictureUrl');
    expect(prisma.review.count).toHaveBeenCalledWith({
      where: { reservation: { courtId: { in: ['court-a', 'court-b'] } } }
    });
  });

  it('scopes the listing to a single court when courtId is provided and owned', async () => {
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue([{ id: 'court-a' }, { id: 'court-b' }])
      },
      review: {
        findMany: jest.fn().mockResolvedValue([]),
        count: jest.fn().mockResolvedValue(0)
      },
      $queryRaw: jest.fn().mockResolvedValue([{ average: null }])
    };
    const repository = new PrismaReviewRepository(prisma as never);

    const result = await repository.listOwnerCourtReviews({
      ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
      court: { courtId: 'court-a' },
      pagination: { page: 1, pageSize: 10 }
    });

    expect(result.summary.selectedCourtId).toBe('court-a');
    expect(result.items).toEqual([]);
    expect(prisma.review.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { reservation: { courtId: { in: ['court-a'] } } }
      })
    );
    expect(prisma.review.count).toHaveBeenCalledWith({
      where: { reservation: { courtId: { in: ['court-a'] } } }
    });

    // The aggregate call must receive only the selected court scope; a
    // regression that still bound every owned court would surface here
    // even if `findMany`/`count` were independently scoped correctly.
    const aggregateCall = prisma.$queryRaw.mock.calls[0]?.[0] as
      | { values?: ReadonlyArray<unknown> }
      | undefined;
    expect(aggregateCall).toBeDefined();
    expect(aggregateCall?.values).toEqual(['court-a']);
  });

  it('throws a not-accessible error when the requested court is not owned', async () => {
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue([{ id: 'court-a' }])
      },
      review: {
        findMany: jest.fn(),
        count: jest.fn()
      },
      $queryRaw: jest.fn()
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await expect(
      repository.listOwnerCourtReviews({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        court: { courtId: 'foreign-court-id' },
        pagination: { page: 1, pageSize: 10 }
      })
    ).rejects.toBeInstanceOf(OwnerReviewCourtNotAccessibleError);

    expect(prisma.review.findMany).not.toHaveBeenCalled();
    expect(prisma.review.count).not.toHaveBeenCalled();
    expect(prisma.$queryRaw).not.toHaveBeenCalled();
  });

  it('throws a not-accessible error when a courtId is provided but the owner has no courts', async () => {
    // Regression guard: previously the repository returned an empty result
    // whenever the owner owned zero courts, even when the caller pinned a
    // specific `courtId`. That masked foreign-court lookups as 200/empty
    // instead of 404. The check must run BEFORE the empty-owned-courts
    // shortcut.
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue([])
      },
      review: {
        findMany: jest.fn(),
        count: jest.fn()
      },
      $queryRaw: jest.fn()
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await expect(
      repository.listOwnerCourtReviews({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        court: { courtId: 'some-court-id' },
        pagination: { page: 1, pageSize: 10 }
      })
    ).rejects.toBeInstanceOf(OwnerReviewCourtNotAccessibleError);

    expect(prisma.review.findMany).not.toHaveBeenCalled();
    expect(prisma.review.count).not.toHaveBeenCalled();
    expect(prisma.$queryRaw).not.toHaveBeenCalled();
  });

  it('applies pagination skip and take derived from page and pageSize', async () => {
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue([{ id: 'court-a' }])
      },
      review: {
        findMany: jest.fn().mockResolvedValue([]),
        count: jest.fn().mockResolvedValue(0)
      },
      $queryRaw: jest.fn().mockResolvedValue([{ average: null }])
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await repository.listOwnerCourtReviews({
      ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
      pagination: { page: 3, pageSize: 5 }
    });

    expect(prisma.review.findMany).toHaveBeenCalledWith(
      expect.objectContaining({ skip: 10, take: 5 })
    );
  });

  it('falls back to the Cognito native provider when none is supplied', async () => {
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue([])
      },
      review: {
        findMany: jest.fn(),
        count: jest.fn()
      },
      $queryRaw: jest.fn()
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await repository.listOwnerCourtReviews({
      ownerIdentity: { sub: 'owner-sub' },
      pagination: { page: 1, pageSize: 10 }
    });

    expect(prisma.court.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining({
          complex: expect.objectContaining({
            owner: expect.objectContaining({
              identities: expect.objectContaining({
                some: { provider: 'Cognito', providerSubject: 'owner-sub' }
              })
            })
          })
        })
      })
    );
  });

  it('rounds the aggregate average to one decimal place', async () => {
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue([{ id: 'court-a' }])
      },
      review: {
        findMany: jest.fn().mockResolvedValue([]),
        count: jest.fn().mockResolvedValue(0)
      },
      $queryRaw: jest.fn().mockResolvedValue([{ average: 4.6666666 }])
    };
    const repository = new PrismaReviewRepository(prisma as never);

    const result = await repository.listOwnerCourtReviews({
      ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
      pagination: { page: 1, pageSize: 10 }
    });

    expect(result.summary.averageRating).toBe(4.7);
  });

  it('returns null average when the aggregate average is null', async () => {
    const prisma = {
      court: {
        findMany: jest.fn().mockResolvedValue([{ id: 'court-a' }])
      },
      review: {
        findMany: jest.fn().mockResolvedValue([]),
        count: jest.fn().mockResolvedValue(0)
      },
      $queryRaw: jest.fn().mockResolvedValue([{ average: null }])
    };
    const repository = new PrismaReviewRepository(prisma as never);

    const result = await repository.listOwnerCourtReviews({
      ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
      pagination: { page: 1, pageSize: 10 }
    });

    expect(result.summary.averageRating).toBeNull();
  });
});

describe('reviews module — create review and latest reviewable reservation', () => {
  const player = {
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
      listOwnerCourtReviews: jest.fn(),
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
    } as unknown as jest.Mocked<IReviewRepository>;
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
    } as unknown as jest.Mocked<IImageUploadRepository>;
  }

  it('creates a 1-star review when comment and evidence image are provided', async () => {
    const reviewRepository = createReviewRepository();
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(player, {
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
      useCase.execute(player, {
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
      useCase.execute(player, {
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
      useCase.execute(player, {
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
      useCase.execute(player, {
        reservationId: 'reservation-id',
        rating: 1,
        comment: 'La iluminación falló toda la hora.',
        evidenceImageUploadId: 'evidence-image-id'
      })
    ).rejects.toThrow('authenticated');
  });

  it('rejects evidence images already assigned to another review', async () => {
    const reviewRepository = createReviewRepository();
    (reviewRepository.findReviewIdByEvidenceImageUploadId as jest.Mock).mockResolvedValue(
      'another-review-id'
    );
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(player, {
        reservationId: 'reservation-id',
        rating: 1,
        comment: 'La iluminación falló toda la hora.',
        evidenceImageUploadId: 'evidence-image-id'
      })
    ).rejects.toThrow('already assigned');
  });

  it('rejects duplicate reviews for the same reservation', async () => {
    const reviewRepository = createReviewRepository();
    (reviewRepository.findReservationById as jest.Mock).mockResolvedValue({
      ...completedReservation,
      reviewId: 'existing-review-id'
    });
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(player, {
        reservationId: 'reservation-id',
        rating: 5
      })
    ).rejects.toThrow('already has a review');
  });

  it('rejects reservations that do not belong to the authenticated user', async () => {
    const reviewRepository = createReviewRepository();
    (reviewRepository.findReservationById as jest.Mock).mockResolvedValue({
      ...completedReservation,
      userId: 'other-user-id'
    });
    const useCase = new CreateReviewUseCase(
      reviewRepository,
      createImageUploadRepository(),
      createSyncAuthenticatedUserUseCase()
    );

    await expect(
      useCase.execute(player, {
        reservationId: 'reservation-id',
        rating: 5
      })
    ).rejects.toThrow('reservation');
  });

  it('rejects reservations that are not completed yet', async () => {
    const reviewRepository = createReviewRepository();
    (reviewRepository.findReservationById as jest.Mock).mockResolvedValue({
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
      useCase.execute(player, {
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
        useCase.execute(player, {
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

    await expect(useCase.execute(player)).resolves.toMatchObject({
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

    await expect(useCase.execute(player)).resolves.toEqual({
      reservationId: 'reservation-id',
      complexName: 'Moravia FC',
      courtName: 'Cancha A',
      startsAt: '2026-07-02T20:00:00.000Z',
      endsAt: '2026-07-02T21:00:00.000Z',
      imageUrl: undefined
    });
  });
});

describe('public court reviews', () => {
  const courtId = '0dd3a274-7d7b-45c6-a90d-4d14298ae7aa';

  const publicResult: IListPublicCourtReviewsResult = {
    summary: { totalReviews: 2, averageRating: 4.5 },
    items: [
      {
        reviewId: 'review-a',
        rating: 5,
        comment: 'Great court and lighting.',
        createdAt: '2026-07-02T18:00:00.000Z',
        reviewer: { displayName: 'Diego R.', initials: 'DR' }
      }
    ],
    totalItems: 2,
    page: 1,
    pageSize: 10
  };

  it('forwards the visible court query through the repository port', async () => {
    const repository = {
      findPubliclyVisibleCourtId: jest.fn().mockResolvedValue(courtId),
      listPublicCourtReviews: jest.fn().mockResolvedValue(publicResult)
    } satisfies Partial<IReviewRepository>;
    const useCase = new ListPublicCourtReviewsUseCase(
      repository as unknown as IReviewRepository
    );

    await expect(
      useCase.execute({ courtId, page: 2, pageSize: 5 })
    ).resolves.toEqual(publicResult);

    expect(repository.findPubliclyVisibleCourtId).toHaveBeenCalledWith(courtId);
    expect(repository.listPublicCourtReviews).toHaveBeenCalledWith({
      courtId,
      pagination: { page: 2, pageSize: 5 }
    });
  });

  it('throws a not-found error when the court is not publicly visible', async () => {
    const repository = {
      findPubliclyVisibleCourtId: jest.fn().mockResolvedValue(null),
      listPublicCourtReviews: jest.fn()
    } satisfies Partial<IReviewRepository>;
    const useCase = new ListPublicCourtReviewsUseCase(
      repository as unknown as IReviewRepository
    );

    await expect(
      useCase.execute({ courtId, page: 1, pageSize: 10 })
    ).rejects.toBeInstanceOf(PublicCourtReviewsCourtNotFoundError);

    expect(repository.listPublicCourtReviews).not.toHaveBeenCalled();
  });

  it('resolves the visible court only against active, published, non-deleted scopes', async () => {
    const prisma = {
      court: {
        findFirst: jest.fn().mockResolvedValue({ id: courtId })
      },
      review: { findMany: jest.fn(), count: jest.fn() },
      $queryRaw: jest.fn()
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await expect(repository.findPubliclyVisibleCourtId(courtId)).resolves.toBe(courtId);

    expect(prisma.court.findFirst).toHaveBeenCalledWith({
      where: {
        id: courtId,
        status: 'ACTIVE',
        deletedAt: null,
        isPublished: true,
        complex: {
          status: 'ACTIVE',
          deletedAt: null,
          isPublished: true
        }
      },
      select: { id: true }
    });
  });

  it('returns null when no publicly visible court matches', async () => {
    const prisma = {
      court: { findFirst: jest.fn().mockResolvedValue(null) },
      review: { findMany: jest.fn(), count: jest.fn() },
      $queryRaw: jest.fn()
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await expect(repository.findPubliclyVisibleCourtId(courtId)).resolves.toBeNull();
  });

  it('maps public review rows and aggregate into the read model', async () => {
    const reviewRows = [
      {
        id: 'review-a',
        rating: 5,
        comment: 'Great court and lighting.',
        createdAt: new Date('2026-07-02T18:00:00.000Z'),
        reservation: { user: { name: 'Diego Rivera' } }
      },
      {
        id: 'review-b',
        rating: 4,
        comment: null,
        createdAt: new Date('2026-07-01T18:00:00.000Z'),
        reservation: { user: { name: null } }
      }
    ];
    const prisma = {
      court: { findFirst: jest.fn() },
      review: {
        findMany: jest.fn().mockResolvedValue(reviewRows),
        count: jest.fn().mockResolvedValue(2)
      },
      $queryRaw: jest.fn().mockResolvedValue([{ average: 4.5 }])
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await expect(
      repository.listPublicCourtReviews({
        courtId,
        pagination: { page: 1, pageSize: 10 }
      })
    ).resolves.toEqual({
      summary: { totalReviews: 2, averageRating: 4.5 },
      items: [
        {
          reviewId: 'review-a',
          rating: 5,
          comment: 'Great court and lighting.',
          createdAt: '2026-07-02T18:00:00.000Z',
          reviewer: { displayName: 'Diego R.', initials: 'DR' }
        },
        {
          reviewId: 'review-b',
          rating: 4,
          comment: null,
          createdAt: '2026-07-01T18:00:00.000Z',
          reviewer: { displayName: 'Player', initials: 'PP' }
        }
      ],
      totalItems: 2,
      page: 1,
      pageSize: 10
    });

    expect(prisma.review.count).toHaveBeenCalledWith({
      where: { reservation: { courtId } }
    });
    // Privacy guard: only the reviewer name is pulled, never PII columns.
    const selectedUser =
      prisma.review.findMany.mock.calls[0]?.[0].select?.reservation?.select?.user;
    expect(selectedUser).not.toHaveProperty('email');
    expect(selectedUser).not.toHaveProperty('pictureUrl');

    // The aggregate call must be scoped to the single requested court.
    const aggregateCall = prisma.$queryRaw.mock.calls[0]?.[0] as
      | { values?: ReadonlyArray<unknown> }
      | undefined;
    expect(aggregateCall?.values).toEqual([courtId]);
  });

  it('returns an empty summary and list when the court has no reviews', async () => {
    const prisma = {
      court: { findFirst: jest.fn() },
      review: {
        findMany: jest.fn().mockResolvedValue([]),
        count: jest.fn().mockResolvedValue(0)
      },
      $queryRaw: jest.fn().mockResolvedValue([{ average: null }])
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await expect(
      repository.listPublicCourtReviews({
        courtId,
        pagination: { page: 1, pageSize: 10 }
      })
    ).resolves.toEqual({
      summary: { totalReviews: 0, averageRating: null },
      items: [],
      totalItems: 0,
      page: 1,
      pageSize: 10
    });
  });

  it('applies pagination skip and take derived from page and pageSize', async () => {
    const prisma = {
      court: { findFirst: jest.fn() },
      review: {
        findMany: jest.fn().mockResolvedValue([]),
        count: jest.fn().mockResolvedValue(0)
      },
      $queryRaw: jest.fn().mockResolvedValue([{ average: null }])
    };
    const repository = new PrismaReviewRepository(prisma as never);

    await repository.listPublicCourtReviews({
      courtId,
      pagination: { page: 3, pageSize: 5 }
    });

    expect(prisma.review.findMany).toHaveBeenCalledWith(
      expect.objectContaining({ skip: 10, take: 5 })
    );
  });

  it('builds the public reviews HTTP response with pagination metadata', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({
        ...publicResult,
        totalItems: 25,
        page: 2,
        pageSize: 10
      })
    } as unknown as ListPublicCourtReviewsUseCase;
    const controller = new PublicCourtReviewsController(useCase);

    const response = await controller.list(courtId, { page: 2, pageSize: 10 });

    expect(response.data).toEqual({
      summary: publicResult.summary,
      items: publicResult.items
    });
    expect(response.meta?.pagination).toEqual({
      page: 2,
      pageSize: 10,
      totalItems: 25,
      totalPages: 3
    });
    expect(useCase.execute).toHaveBeenCalledWith({
      courtId,
      page: 2,
      pageSize: 10
    });
  });

  it('caps the page size at 50 and defaults to 10', () => {
    expect(PUBLIC_COURT_REVIEWS_MAX_PAGE_SIZE).toBe(50);
    expect(PUBLIC_COURT_REVIEWS_DEFAULT_PAGE_SIZE).toBe(10);
  });

  it('caps the page number at 10000 to prevent huge offset abuse', async () => {
    expect(PUBLIC_COURT_REVIEWS_MAX_PAGE).toBe(10_000);

    const oversized = plainToInstance(ListPublicCourtReviewsQuery, {
      page: PUBLIC_COURT_REVIEWS_MAX_PAGE + 1,
      pageSize: 10
    });
    const errors = await validate(oversized);
    expect(errors).toHaveLength(1);
    expect(errors[0]?.property).toBe('page');
    expect(errors[0]?.constraints?.max).toBeDefined();
  });
});
