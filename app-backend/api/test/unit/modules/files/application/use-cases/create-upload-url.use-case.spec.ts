import { CreateUploadUrlUseCase } from '@/modules/files/application/use-cases/create-upload-url.use-case';
import type { IFileStoragePort } from '@/modules/files/application/ports/file-storage.port';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { ImageUploadPolicyService } from '@/modules/files/domain/services/image-upload-policy.service';

describe('CreateUploadUrlUseCase', () => {
  it('creates a presigned upload URL for a validated profile image', async () => {
    const storage = {
      createPresignedUploadUrl: jest.fn().mockResolvedValue({
        method: 'POST',
        uploadUrl: 'https://upload.example.test',
        fields: {
          key: 'dev/profile-image/cognito-sub/file.jpg',
          policy: 'policy'
        }
      }),
      inspectUploadedObject: jest.fn(),
      createPresignedReadUrl: jest.fn()
    } satisfies IFileStoragePort;
    const policy = new ImageUploadPolicyService({
      allowedMimeTypes: ['image/jpeg'],
      profileImageMaxBytes: 1024,
      keyPrefix: 'dev'
    });
    const useCase = new CreateUploadUrlUseCase(storage, policy, 120);

    const result = await useCase.execute({
      ownerSub: 'cognito-sub',
      purpose: FilePurpose.ProfileImage,
      contentType: 'image/jpeg',
      sizeBytes: 100
    });

    expect(result).toEqual({
      objectKey: expect.stringMatching(
        /^dev\/profile-image\/cognito-sub\/\d{4}\/\d{2}\/.+\.jpg$/
      ),
      method: 'POST',
      uploadUrl: 'https://upload.example.test',
      fields: {
        key: 'dev/profile-image/cognito-sub/file.jpg',
        policy: 'policy'
      },
      expiresInSeconds: 120,
      maxSizeBytes: 1024
    });
    expect(storage.createPresignedUploadUrl).toHaveBeenCalledWith({
      objectKey: result.objectKey,
      contentType: 'image/jpeg',
      expiresInSeconds: 120,
      maxSizeBytes: 1024
    });
  });

  it('creates a presigned upload URL for a review evidence image', async () => {
    const storage = {
      createPresignedUploadUrl: jest.fn().mockResolvedValue({
        method: 'POST',
        uploadUrl: 'https://upload.example.test',
        fields: {
          key: 'dev/review-evidence-image/cognito-sub/file.jpg',
          policy: 'policy'
        }
      }),
      inspectUploadedObject: jest.fn(),
      createPresignedReadUrl: jest.fn()
    } satisfies IFileStoragePort;
    const policy = new ImageUploadPolicyService({
      allowedMimeTypes: ['image/jpeg'],
      profileImageMaxBytes: 1024,
      keyPrefix: 'dev'
    });
    const useCase = new CreateUploadUrlUseCase(storage, policy, 120);

    const result = await useCase.execute({
      ownerSub: 'cognito-sub',
      purpose: FilePurpose.ReviewEvidenceImage,
      contentType: 'image/jpeg',
      sizeBytes: 100
    });

    expect(result.objectKey).toMatch(
      /^dev\/review-evidence-image\/cognito-sub\/\d{4}\/\d{2}\/.+\.jpg$/
    );
  });
});
