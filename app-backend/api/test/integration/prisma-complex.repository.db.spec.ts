import { randomUUID } from 'node:crypto';

import { PrismaComplexRepository } from '@/modules/complexes/infrastructure/persistence/prisma-complex.repository';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import type { IFileReadUrlPort } from '@/modules/files/application/ports/file-read-url.port';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

const DEFAULT_TEST_DATABASE_URL = 'postgresql://user:password@localhost:5432/appdb';
const liveDatabaseUrl = process.env.DATABASE_URL;
const runLiveDatabaseIntegration =
  typeof liveDatabaseUrl === 'string' &&
  liveDatabaseUrl.length > 0 &&
  liveDatabaseUrl !== DEFAULT_TEST_DATABASE_URL;

(runLiveDatabaseIntegration ? describe : describe.skip)(
  'PrismaComplexRepository live DB integration',
  () => {
    let prismaService: PrismaService;

    beforeAll(async () => {
      prismaService = new PrismaService();
      await prismaService.onModuleInit();
    });

    afterAll(async () => {
      await prismaService?.onModuleDestroy();
    });

    it('updates an owned court image when the provided complex id is stale but the court still belongs to the owner', async () => {
      const fixture = await seedOwnedCourtFixture(prismaService);
      const fileReadUrl: IFileReadUrlPort = {
        createReadUrl: jest
          .fn()
          .mockImplementation(async (objectKey: string) => `https://signed.example.test/${objectKey}`)
      };
      const repository = new PrismaComplexRepository(prismaService as never, fileReadUrl);

      try {
        await expect(
          repository.getMyComplexHub({
            ownerIdentity: {
              sub: fixture.ownerSub,
              provider: fixture.ownerProvider
            }
          })
        ).resolves.toEqual({
          complexes: [
            expect.objectContaining({
              id: fixture.actualComplexId,
              courts: [expect.objectContaining({ id: fixture.courtId, imageUrl: null })]
            })
          ]
        });

        await expect(
          repository.updateOwnedCourtImage({
            ownerIdentity: {
              sub: fixture.ownerSub,
              provider: fixture.ownerProvider
            },
            complexId: fixture.staleComplexId,
            courtId: fixture.courtId,
            imageUploadId: fixture.imageUploadId
          })
        ).resolves.toEqual({
          id: fixture.courtId,
          name: fixture.courtName,
          status: 'ACTIVE',
          availabilityStatus: 'PENDING',
          imageUrl: `https://signed.example.test/${fixture.objectKey}`
        });

        await expect(
          prismaService.court.findUnique({
            where: { id: fixture.courtId },
            select: { imageUploadId: true }
          })
        ).resolves.toEqual({ imageUploadId: fixture.imageUploadId });
      } finally {
        await cleanupOwnedCourtFixture(prismaService, fixture);
      }
    });

    it('updates an owned court image when the request provider differs but the Cognito subject still belongs to the owner', async () => {
      const fixture = await seedOwnedCourtFixture(prismaService);
      const fileReadUrl: IFileReadUrlPort = {
        createReadUrl: jest
          .fn()
          .mockImplementation(async (objectKey: string) => `https://signed.example.test/${objectKey}`)
      };
      const repository = new PrismaComplexRepository(prismaService as never, fileReadUrl);

      try {
        await expect(
          repository.updateOwnedCourtImage({
            ownerIdentity: {
              sub: fixture.ownerSub,
              provider: 'Cognito'
            },
            complexId: fixture.actualComplexId,
            courtId: fixture.courtId,
            imageUploadId: fixture.imageUploadId
          })
        ).resolves.toEqual({
          id: fixture.courtId,
          name: fixture.courtName,
          status: 'ACTIVE',
          availabilityStatus: 'PENDING',
          imageUrl: `https://signed.example.test/${fixture.objectKey}`
        });
      } finally {
        await cleanupOwnedCourtFixture(prismaService, fixture);
      }
    });
  }
);

interface IOwnedCourtFixture {
  ownerId: string;
  ownerSub: string;
  ownerProvider: string;
  actualComplexId: string;
  staleComplexId: string;
  courtId: string;
  courtName: string;
  imageUploadId: string;
  objectKey: string;
}

async function seedOwnedCourtFixture(prismaService: PrismaService): Promise<IOwnedCourtFixture> {
  const suffix = randomUUID().slice(0, 8);
  const fixture: IOwnedCourtFixture = {
    ownerId: randomUUID(),
    ownerSub: `owner-sub-${suffix}`,
    ownerProvider: 'Google',
    actualComplexId: randomUUID(),
    staleComplexId: randomUUID(),
    courtId: randomUUID(),
    courtName: 'Legacy Court',
    imageUploadId: randomUUID(),
    objectKey: `test/uploads/court-image/owner-sub-${suffix}/2026/07/${randomUUID()}.png`
  };

  await prismaService.user.create({
    data: {
      id: fixture.ownerId,
      email: `complex-owner-${suffix}@example.test`,
      name: 'Complex Owner',
      identities: {
        create: {
          provider: fixture.ownerProvider,
          providerSubject: fixture.ownerSub,
          emailAtLogin: `complex-owner-${suffix}@example.test`
        }
      }
    }
  });

  await prismaService.complex.create({
    data: {
      id: fixture.actualComplexId,
      ownerId: fixture.ownerId,
      name: 'Legacy Complex',
      address: '123 Legacy Street'
    }
  });

  await prismaService.court.create({
    data: {
      id: fixture.courtId,
      complexId: fixture.actualComplexId,
      name: fixture.courtName
    }
  });

  await prismaService.imageUpload.create({
    data: {
      id: fixture.imageUploadId,
      ownerSub: fixture.ownerSub,
      ownerEmail: `complex-owner-${suffix}@example.test`,
      ownerName: 'Complex Owner',
      ownerProvider: fixture.ownerProvider,
      purpose: FilePurpose.CourtImage,
      objectKey: fixture.objectKey,
      contentType: 'image/png',
      sizeBytes: 2048
    }
  });

  return fixture;
}

async function cleanupOwnedCourtFixture(
  prismaService: PrismaService,
  fixture: IOwnedCourtFixture
): Promise<void> {
  await prismaService.court.deleteMany({ where: { id: fixture.courtId } });
  await prismaService.imageUpload.deleteMany({ where: { id: fixture.imageUploadId } });
  await prismaService.complex.deleteMany({ where: { id: fixture.actualComplexId } });
  await prismaService.user.deleteMany({ where: { id: fixture.ownerId } });
}
