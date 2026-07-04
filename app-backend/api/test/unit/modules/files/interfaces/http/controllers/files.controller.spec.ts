import type { ConfirmUploadUseCase } from '@/modules/files/application/use-cases/confirm-upload.use-case';
import type { CreateUploadUrlUseCase } from '@/modules/files/application/use-cases/create-upload-url.use-case';
import type { ListImageUploadsUseCase } from '@/modules/files/application/use-cases/list-image-uploads.use-case';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { FileTooLargeError } from '@/modules/files/domain/errors/file-too-large.error';
import { InvalidFilePurposeError } from '@/modules/files/domain/errors/invalid-file-purpose.error';
import { UnsupportedFileTypeError } from '@/modules/files/domain/errors/unsupported-file-type.error';
import { FilesController } from '@/modules/files/interfaces/http/controllers/files.controller';

describe('FilesController', () => {
  function createListUseCase(): ListImageUploadsUseCase {
    return {
      execute: jest.fn()
    } as unknown as ListImageUploadsUseCase;
  }

  it('delegates upload URL creation to the use case', async () => {
    const response = {
      objectKey: 'dev/profile-image/sub/2026/06/file.jpg',
      uploadUrl: 'https://upload.example.test',
      method: 'POST',
      fields: {
        key: 'dev/profile-image/sub/file.jpg',
        policy: 'policy'
      },
      expiresInSeconds: 300,
      maxSizeBytes: 5242880
    };
    const useCase = {
      execute: jest.fn().mockResolvedValue(response)
    } as unknown as CreateUploadUrlUseCase;
    const confirmUseCase = {
      execute: jest.fn()
    } as unknown as ConfirmUploadUseCase;
    const controller = new FilesController(useCase, confirmUseCase, createListUseCase());

    await expect(
      controller.createUpload(
        { sub: 'sub', groups: [] },
        {
          purpose: FilePurpose.ProfileImage,
          contentType: 'image/jpeg',
          sizeBytes: 100
        }
      )
    ).resolves.toEqual(response);
    expect(useCase.execute).toHaveBeenCalledWith({
      ownerSub: 'sub',
      purpose: FilePurpose.ProfileImage,
      contentType: 'image/jpeg',
      sizeBytes: 100
    });
  });

  it('delegates court image upload URL creation to the use case', async () => {
    const response = {
      objectKey: 'dev/court-image/sub/2026/06/file.jpg',
      uploadUrl: 'https://upload.example.test',
      method: 'POST',
      fields: {
        key: 'dev/court-image/sub/file.jpg',
        policy: 'policy'
      },
      expiresInSeconds: 300,
      maxSizeBytes: 5242880
    };
    const useCase = {
      execute: jest.fn().mockResolvedValue(response)
    } as unknown as CreateUploadUrlUseCase;
    const controller = new FilesController(
      useCase,
      { execute: jest.fn() } as unknown as ConfirmUploadUseCase,
      createListUseCase()
    );

    await expect(
      controller.createUpload(
        { sub: 'sub', groups: [] },
        {
          purpose: FilePurpose.CourtImage,
          contentType: 'image/jpeg',
          sizeBytes: 100
        }
      )
    ).resolves.toEqual(response);
    expect(useCase.execute).toHaveBeenCalledWith({
      ownerSub: 'sub',
      purpose: FilePurpose.CourtImage,
      contentType: 'image/jpeg',
      sizeBytes: 100
    });
  });

  it('delegates review evidence image upload URL creation to the use case', async () => {
    const response = {
      objectKey: 'dev/review-evidence-image/sub/2026/06/file.jpg',
      uploadUrl: 'https://upload.example.test',
      method: 'POST',
      fields: {
        key: 'dev/review-evidence-image/sub/file.jpg',
        policy: 'policy'
      },
      expiresInSeconds: 300,
      maxSizeBytes: 5242880
    };
    const useCase = {
      execute: jest.fn().mockResolvedValue(response)
    } as unknown as CreateUploadUrlUseCase;
    const controller = new FilesController(
      useCase,
      { execute: jest.fn() } as unknown as ConfirmUploadUseCase,
      createListUseCase()
    );

    await expect(
      controller.createUpload(
        { sub: 'sub', groups: [] },
        {
          purpose: FilePurpose.ReviewEvidenceImage,
          contentType: 'image/jpeg',
          sizeBytes: 100
        }
      )
    ).resolves.toEqual(response);
  });

  it('propagates unsupported image type errors to the global filter', async () => {
    const useCase = {
      execute: jest
        .fn()
        .mockRejectedValue(new UnsupportedFileTypeError('image/svg+xml'))
    } as unknown as CreateUploadUrlUseCase;
    const controller = new FilesController(
      useCase,
      { execute: jest.fn() } as unknown as ConfirmUploadUseCase,
      createListUseCase()
    );

    await expect(
      controller.createUpload(
        { sub: 'sub', groups: [] },
        {
          purpose: FilePurpose.ProfileImage,
          contentType: 'image/svg+xml',
          sizeBytes: 100
        }
      )
    ).rejects.toBeInstanceOf(UnsupportedFileTypeError);
  });

  it('propagates oversized image errors to the global filter', async () => {
    const useCase = {
      execute: jest.fn().mockRejectedValue(new FileTooLargeError(2048, 1024))
    } as unknown as CreateUploadUrlUseCase;
    const controller = new FilesController(
      useCase,
      { execute: jest.fn() } as unknown as ConfirmUploadUseCase,
      createListUseCase()
    );

    await expect(
      controller.createUpload(
        { sub: 'sub', groups: [] },
        {
          purpose: FilePurpose.ProfileImage,
          contentType: 'image/jpeg',
          sizeBytes: 2048
        }
      )
    ).rejects.toBeInstanceOf(FileTooLargeError);
  });

  it('propagates invalid purpose errors to the global filter', async () => {
    const useCase = {
      execute: jest
        .fn()
        .mockRejectedValue(new InvalidFilePurposeError('banner-image'))
    } as unknown as CreateUploadUrlUseCase;
    const controller = new FilesController(
      useCase,
      { execute: jest.fn() } as unknown as ConfirmUploadUseCase,
      createListUseCase()
    );

    await expect(
      controller.createUpload(
        { sub: 'sub', groups: [] },
        {
          purpose: 'banner-image' as FilePurpose,
          contentType: 'image/jpeg',
          sizeBytes: 100
        }
      )
    ).rejects.toBeInstanceOf(InvalidFilePurposeError);
  });

  it('delegates upload confirmation to the use case', async () => {
    const response = {
      id: 'image-id',
      objectKey: 'dev/uploads/profile-image/sub/2026/06/file.jpg',
      purpose: FilePurpose.ProfileImage,
      status: 'ready' as const,
      contentType: 'image/jpeg',
      sizeBytes: 100,
      readUrl: 'https://read.example.test/file.jpg',
      createdAt: '2026-06-11T00:00:00.000Z',
      uploadedBy: {
        sub: 'sub',
        email: 'user@example.test',
        name: 'User Name',
        provider: 'Google'
      }
    };
    const confirmUseCase = {
      execute: jest.fn().mockResolvedValue(response)
    } as unknown as ConfirmUploadUseCase;
    const controller = new FilesController(
      { execute: jest.fn() } as unknown as CreateUploadUrlUseCase,
      confirmUseCase,
      createListUseCase()
    );

    await expect(
      controller.confirm(
        {
          sub: 'sub',
          email: 'user@example.test',
          name: 'User Name',
          provider: 'Google',
          groups: []
        },
        {
          purpose: FilePurpose.ProfileImage,
          objectKey: 'dev/uploads/profile-image/sub/2026/06/file.jpg'
        }
      )
    ).resolves.toEqual(response);
    expect(confirmUseCase.execute).toHaveBeenCalledWith({
      ownerSub: 'sub',
      ownerEmail: 'user@example.test',
      ownerName: 'User Name',
      ownerPictureUrl: undefined,
      ownerProvider: 'Google',
      purpose: FilePurpose.ProfileImage,
      objectKey: 'dev/uploads/profile-image/sub/2026/06/file.jpg'
    });
  });

  it('delegates uploaded image listing to the use case', async () => {
    const response = [
      {
        id: 'image-id',
        objectKey: 'dev/uploads/profile-image/sub/2026/06/file.jpg',
        purpose: FilePurpose.ProfileImage,
        contentType: 'image/jpeg',
        sizeBytes: 100,
        readUrl: 'https://read.example.test/file.jpg',
        createdAt: '2026-06-11T00:00:00.000Z',
        uploadedBy: {
          sub: 'sub'
        }
      }
    ];
    const listUseCase = {
      execute: jest.fn().mockResolvedValue(response)
    } as unknown as ListImageUploadsUseCase;
    const controller = new FilesController(
      { execute: jest.fn() } as unknown as CreateUploadUrlUseCase,
      { execute: jest.fn() } as unknown as ConfirmUploadUseCase,
      listUseCase
    );

    await expect(controller.listUploads({ sub: 'sub', groups: [] })).resolves.toEqual(response);
    expect(listUseCase.execute).toHaveBeenCalledWith('sub');
  });
});
