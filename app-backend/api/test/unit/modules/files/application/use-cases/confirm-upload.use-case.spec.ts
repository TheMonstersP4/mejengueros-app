import { ConfirmUploadUseCase } from '@/modules/files/application/use-cases/confirm-upload.use-case';
import type { IFileStoragePort } from '@/modules/files/application/ports/file-storage.port';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { ImageUploadEntity } from '@/modules/files/domain/entities/image-upload.entity';
import { FileOwnershipError } from '@/modules/files/domain/errors/file-ownership.error';
import { UnsupportedFileTypeError } from '@/modules/files/domain/errors/unsupported-file-type.error';
import { ImageUploadPolicyService } from '@/modules/files/domain/services/image-upload-policy.service';

describe('ConfirmUploadUseCase', () => {
  function createPolicy(): ImageUploadPolicyService {
    return new ImageUploadPolicyService({
      allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp'],
      profileImageMaxBytes: 1024,
      keyPrefix: 'dev/uploads'
    });
  }

  function createRepository(): jest.Mocked<IImageUploadRepository> {
    return {
      findById: jest.fn(),
      saveConfirmedUpload: jest.fn().mockResolvedValue(
        ImageUploadEntity.fromPersistence({
          id: 'image-id',
          ownerSub: 'cognito-sub',
          ownerEmail: 'user@example.test',
          ownerName: 'User Name',
          ownerPictureUrl: 'https://example.test/avatar.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.ProfileImage,
          objectKey: 'dev/uploads/profile-image/cognito-sub/2026/06/file.jpg',
          contentType: 'image/jpeg',
          sizeBytes: 512,
          createdAt: new Date('2026-06-11T00:00:00.000Z')
        })
      ),
      listRecent: jest.fn()
    };
  }

  it('confirms an uploaded profile image when storage metadata is valid', async () => {
    const storage = {
      createPresignedUploadUrl: jest.fn(),
      inspectUploadedObject: jest.fn().mockResolvedValue({
        contentType: 'image/jpeg',
        sizeBytes: 512,
        detectedContentType: 'image/jpeg'
      }),
      createPresignedReadUrl: jest.fn().mockResolvedValue({
        readUrl: 'https://read.example.test/file.jpg'
      })
    } satisfies IFileStoragePort;
    const repository = createRepository();
    const useCase = new ConfirmUploadUseCase(storage, createPolicy(), repository, 300);

    await expect(
      useCase.execute({
        ownerSub: 'cognito-sub',
        ownerEmail: 'user@example.test',
        ownerName: 'User Name',
        ownerPictureUrl: 'https://example.test/avatar.png',
        ownerProvider: 'Google',
        purpose: FilePurpose.ProfileImage,
        objectKey: 'dev/uploads/profile-image/cognito-sub/2026/06/file.jpg'
      })
    ).resolves.toEqual({
      id: 'image-id',
      objectKey: 'dev/uploads/profile-image/cognito-sub/2026/06/file.jpg',
      purpose: FilePurpose.ProfileImage,
      status: 'ready',
      contentType: 'image/jpeg',
      sizeBytes: 512,
      readUrl: 'https://read.example.test/file.jpg',
      createdAt: '2026-06-11T00:00:00.000Z',
      uploadedBy: {
        sub: 'cognito-sub',
        email: 'user@example.test',
        name: 'User Name',
        pictureUrl: 'https://example.test/avatar.png',
        provider: 'Google'
      }
    });
    expect(storage.inspectUploadedObject).toHaveBeenCalledWith({
      objectKey: 'dev/uploads/profile-image/cognito-sub/2026/06/file.jpg'
    });
    expect(repository.saveConfirmedUpload).toHaveBeenCalledWith({
      ownerSub: 'cognito-sub',
      ownerEmail: 'user@example.test',
      ownerName: 'User Name',
      ownerPictureUrl: 'https://example.test/avatar.png',
      ownerProvider: 'Google',
      purpose: FilePurpose.ProfileImage,
      objectKey: 'dev/uploads/profile-image/cognito-sub/2026/06/file.jpg',
      contentType: 'image/jpeg',
      sizeBytes: 512
    });
  });

  it('confirms an uploaded court image when storage metadata is valid', async () => {
    const storage = {
      createPresignedUploadUrl: jest.fn(),
      inspectUploadedObject: jest.fn().mockResolvedValue({
        contentType: 'image/png',
        sizeBytes: 512,
        detectedContentType: 'image/png'
      }),
      createPresignedReadUrl: jest.fn().mockResolvedValue({
        readUrl: 'https://read.example.test/court.png'
      })
    } satisfies IFileStoragePort;
    const repository = createRepository();
    repository.saveConfirmedUpload.mockResolvedValueOnce(
      ImageUploadEntity.fromPersistence({
        id: 'court-image-id',
        ownerSub: 'cognito-sub',
        ownerEmail: 'user@example.test',
        ownerName: 'User Name',
        ownerPictureUrl: 'https://example.test/avatar.png',
        ownerProvider: 'Google',
        purpose: FilePurpose.CourtImage,
        objectKey: 'dev/uploads/court-image/cognito-sub/2026/06/court.png',
        contentType: 'image/png',
        sizeBytes: 512,
        createdAt: new Date('2026-06-11T00:00:00.000Z')
      })
    );
    const useCase = new ConfirmUploadUseCase(storage, createPolicy(), repository, 300);

    await expect(
      useCase.execute({
        ownerSub: 'cognito-sub',
        ownerEmail: 'user@example.test',
        ownerName: 'User Name',
        ownerPictureUrl: 'https://example.test/avatar.png',
        ownerProvider: 'Google',
        purpose: FilePurpose.CourtImage,
        objectKey: 'dev/uploads/court-image/cognito-sub/2026/06/court.png'
      })
    ).resolves.toEqual({
      id: 'court-image-id',
      objectKey: 'dev/uploads/court-image/cognito-sub/2026/06/court.png',
      purpose: FilePurpose.CourtImage,
      status: 'ready',
      contentType: 'image/png',
      sizeBytes: 512,
      readUrl: 'https://read.example.test/court.png',
      createdAt: '2026-06-11T00:00:00.000Z',
      uploadedBy: {
        sub: 'cognito-sub',
        email: 'user@example.test',
        name: 'User Name',
        pictureUrl: 'https://example.test/avatar.png',
        provider: 'Google'
      }
    });
  });

  it('confirms an uploaded review evidence image when storage metadata is valid', async () => {
    const storage = {
      createPresignedUploadUrl: jest.fn(),
      inspectUploadedObject: jest.fn().mockResolvedValue({
        contentType: 'image/png',
        sizeBytes: 512,
        detectedContentType: 'image/png'
      }),
      createPresignedReadUrl: jest.fn().mockResolvedValue({
        readUrl: 'https://read.example.test/review-evidence.png'
      })
    } satisfies IFileStoragePort;
    const repository = createRepository();
    repository.saveConfirmedUpload.mockResolvedValueOnce(
      ImageUploadEntity.fromPersistence({
        id: 'review-evidence-image-id',
        ownerSub: 'cognito-sub',
        ownerEmail: 'user@example.test',
        ownerName: 'User Name',
        ownerPictureUrl: 'https://example.test/avatar.png',
        ownerProvider: 'Google',
        purpose: FilePurpose.ReviewEvidenceImage,
        objectKey:
          'dev/uploads/review-evidence-image/cognito-sub/2026/06/review-evidence.png',
        contentType: 'image/png',
        sizeBytes: 512,
        createdAt: new Date('2026-06-11T00:00:00.000Z')
      })
    );
    const useCase = new ConfirmUploadUseCase(storage, createPolicy(), repository, 300);

    await expect(
      useCase.execute({
        ownerSub: 'cognito-sub',
        ownerEmail: 'user@example.test',
        ownerName: 'User Name',
        ownerPictureUrl: 'https://example.test/avatar.png',
        ownerProvider: 'Google',
        purpose: FilePurpose.ReviewEvidenceImage,
        objectKey:
          'dev/uploads/review-evidence-image/cognito-sub/2026/06/review-evidence.png'
      })
    ).resolves.toMatchObject({
      id: 'review-evidence-image-id',
      purpose: FilePurpose.ReviewEvidenceImage,
      readUrl: 'https://read.example.test/review-evidence.png'
    });
  });

  it('rejects object keys that do not belong to the current user before storage access', async () => {
    const storage = {
      createPresignedUploadUrl: jest.fn(),
      inspectUploadedObject: jest.fn(),
      createPresignedReadUrl: jest.fn()
    } satisfies IFileStoragePort;
    const useCase = new ConfirmUploadUseCase(
      storage,
      createPolicy(),
      createRepository(),
      300
    );

    await expect(
      useCase.execute({
        ownerSub: 'current-sub',
        purpose: FilePurpose.ProfileImage,
        objectKey: 'dev/uploads/profile-image/other-sub/2026/06/file.jpg'
      })
    ).rejects.toThrow(FileOwnershipError);
    expect(storage.inspectUploadedObject).not.toHaveBeenCalled();
  });

  it('rejects files whose stored type does not match their byte signature', async () => {
    const storage = {
      createPresignedUploadUrl: jest.fn(),
      inspectUploadedObject: jest.fn().mockResolvedValue({
        contentType: 'image/jpeg',
        sizeBytes: 512,
        detectedContentType: 'image/png'
      }),
      createPresignedReadUrl: jest.fn()
    } satisfies IFileStoragePort;
    const useCase = new ConfirmUploadUseCase(
      storage,
      createPolicy(),
      createRepository(),
      300
    );

    await expect(
      useCase.execute({
        ownerSub: 'cognito-sub',
        purpose: FilePurpose.ProfileImage,
        objectKey: 'dev/uploads/profile-image/cognito-sub/2026/06/file.jpg'
      })
    ).rejects.toThrow(UnsupportedFileTypeError);
  });
});
