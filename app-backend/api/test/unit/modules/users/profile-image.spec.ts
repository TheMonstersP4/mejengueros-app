import type { IFileReadUrlPort } from '@/modules/files/application/ports/file-read-url.port';
import { StorageInspectionError } from '@/modules/files/infrastructure/errors/storage-inspection.error';
import { ImageUploadEntity } from '@/modules/files/domain/entities/image-upload.entity';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import { UserProfileService } from '@/modules/users/application/services/user-profile.service';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { ListUsersUseCase } from '@/modules/users/application/use-cases/list-users.use-case';
import { UpdateMyProfileImageUseCase } from '@/modules/users/application/use-cases/update-my-profile-image.use-case';
import { UserEntity } from '@/modules/users/domain/entities/user.entity';
import { InvalidProfileImageUploadError } from '@/modules/users/domain/errors/invalid-profile-image-upload.error';
import type { IUserRepository } from '@/modules/users/domain/repositories/user.repository';
import { PrismaUserRepository } from '@/modules/users/infrastructure/persistence/prisma-user.repository';

describe('user profile image association', () => {
  const identity = {
    sub: 'cognito-sub',
    email: 'player@example.test',
    emailVerified: true,
    name: 'Player One',
    pictureUrl: 'https://provider.example.test/avatar.png',
    provider: 'Google',
    groups: ['players']
  };

  function createUser(profileImageUploadId?: string): UserEntity {
    return UserEntity.fromPersistence({
      id: 'user-id',
      email: 'player@example.test',
      name: 'Player One',
      pictureUrl: 'https://provider.example.test/avatar.png',
      profileImageUploadId,
      status: 'ACTIVE',
      currentIdentity: {
        provider: 'Google',
        providerSubject: 'cognito-sub'
      },
      roles: ['PLAYER']
    });
  }

  function createUpload(
    purpose: FilePurpose = FilePurpose.ProfileImage,
    ownerSub = 'cognito-sub'
  ): ImageUploadEntity {
    return ImageUploadEntity.fromPersistence({
      id: 'profile-upload-id',
      ownerSub,
      purpose,
      objectKey:
        'dev/uploads/confirmed/profile-image/cognito-sub/2026/07/profile.jpg',
      contentType: 'image/jpeg',
      sizeBytes: 512,
      createdAt: new Date('2026-07-29T18:00:00.000Z')
    });
  }

  function createImageRepository(
    upload: ImageUploadEntity | null = createUpload()
  ): jest.Mocked<IImageUploadRepository> {
    return {
      findById: jest.fn().mockResolvedValue(upload),
      saveConfirmedUpload: jest.fn(),
      listRecent: jest.fn()
    };
  }

  function createUserRepository(): jest.Mocked<IUserRepository> {
    return {
      syncAuthenticatedUser: jest.fn().mockResolvedValue(createUser()),
      findByCognitoSub: jest.fn(),
      replaceProfileImage: jest
        .fn()
        .mockResolvedValue(createUser('profile-upload-id')),
      updateAccount: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    };
  }

  function createReadUrl(): jest.Mocked<IFileReadUrlPort> {
    return {
      createReadUrl: jest
        .fn()
        .mockResolvedValue('https://read.example.test/custom-profile.jpg')
    };
  }

  it('associates an owned confirmed profile upload and returns its signed URL', async () => {
    const users = createUserRepository();
    const images = createImageRepository();
    const readUrl = createReadUrl();
    const profiles = new UserProfileService(images, readUrl);
    const useCase = new UpdateMyProfileImageUseCase(users, images, profiles);

    await expect(
      useCase.execute(identity, 'profile-upload-id')
    ).resolves.toMatchObject({
      id: 'user-id',
      pictureUrl: 'https://read.example.test/custom-profile.jpg'
    });
    expect(users.replaceProfileImage).toHaveBeenCalledWith({
      userId: 'user-id',
      imageUploadId: 'profile-upload-id'
    });
    expect(readUrl.createReadUrl).toHaveBeenCalledWith(
      'dev/uploads/confirmed/profile-image/cognito-sub/2026/07/profile.jpg'
    );
  });

  it('returns the requesting identity when the user links several logins', async () => {
    const users = createUserRepository();
    users.replaceProfileImage.mockResolvedValue(
      UserEntity.fromPersistence({
        id: 'user-id',
        email: 'player@example.test',
        profileImageUploadId: 'profile-upload-id',
        status: 'ACTIVE',
        currentIdentity: {
          provider: 'Cognito',
          providerSubject: 'other-linked-sub'
        },
        roles: ['PLAYER']
      })
    );
    const images = createImageRepository();
    const useCase = new UpdateMyProfileImageUseCase(
      users,
      images,
      new UserProfileService(images, createReadUrl())
    );

    await expect(
      useCase.execute(identity, 'profile-upload-id')
    ).resolves.toMatchObject({
      cognitoSub: 'cognito-sub',
      provider: 'Google'
    });
  });

  it('rejects a missing upload', async () => {
    const users = createUserRepository();
    const images = createImageRepository(null);
    const useCase = new UpdateMyProfileImageUseCase(
      users,
      images,
      new UserProfileService(images, createReadUrl())
    );

    await expect(useCase.execute(identity, 'missing-upload-id')).rejects.toMatchObject({
      code: 'RESOURCE_NOT_FOUND',
      kind: 'not_found'
    });
    expect(users.replaceProfileImage).not.toHaveBeenCalled();
  });

  it('rejects an upload owned by another Cognito subject', async () => {
    const users = createUserRepository();
    const images = createImageRepository(
      createUpload(FilePurpose.ProfileImage, 'other-sub')
    );
    const useCase = new UpdateMyProfileImageUseCase(
      users,
      images,
      new UserProfileService(images, createReadUrl())
    );

    await expect(
      useCase.execute(identity, 'profile-upload-id')
    ).rejects.toMatchObject({ code: 'FORBIDDEN', kind: 'forbidden' });
    expect(users.replaceProfileImage).not.toHaveBeenCalled();
  });

  it('rejects an upload with a non-profile purpose', async () => {
    const users = createUserRepository();
    const images = createImageRepository(createUpload(FilePurpose.CourtImage));
    const useCase = new UpdateMyProfileImageUseCase(
      users,
      images,
      new UserProfileService(images, createReadUrl())
    );

    await expect(
      useCase.execute(identity, 'profile-upload-id')
    ).rejects.toMatchObject({ code: 'VALIDATION_FAILED', kind: 'validation' });
    expect(users.replaceProfileImage).not.toHaveBeenCalled();
  });

  it('uses the provider picture when no custom image is associated', async () => {
    const images = createImageRepository();
    const readUrl = createReadUrl();
    const profiles = new UserProfileService(images, readUrl);

    await expect(profiles.render(createUser())).resolves.toMatchObject({
      pictureUrl: 'https://provider.example.test/avatar.png'
    });
    expect(images.findById).not.toHaveBeenCalled();
    expect(readUrl.createReadUrl).not.toHaveBeenCalled();
  });

  it('propagates signing failures instead of persisting a presigned URL', async () => {
    const images = createImageRepository();
    const signingError = new StorageInspectionError(
      'dev/uploads/confirmed/profile-image/cognito-sub/profile.jpg',
      new Error('signing failed')
    );
    const readUrl = {
      createReadUrl: jest.fn().mockRejectedValue(signingError)
    } satisfies IFileReadUrlPort;
    const profiles = new UserProfileService(images, readUrl);

    await expect(
      profiles.render(createUser('profile-upload-id'))
    ).rejects.toBe(signingError);
  });

  it('uses custom signed images in the shared user-list profile contract', async () => {
    const users = createUserRepository();
    users.list.mockResolvedValue([createUser('profile-upload-id')]);
    const images = createImageRepository();
    const useCase = new ListUsersUseCase(
      users,
      new UserProfileService(images, createReadUrl())
    );

    await expect(
      useCase.execute({ sub: 'admin-sub', groups: ['admin'] })
    ).resolves.toEqual([
      expect.objectContaining({
        pictureUrl: 'https://read.example.test/custom-profile.jpg'
      })
    ]);
  });

  it('replaces the relation with one atomic user update', async () => {
    const updatedUser = {
      id: 'user-id',
      email: 'player@example.test',
      pictureUrl: 'https://provider.example.test/avatar.png',
      profileImageUploadId: 'new-profile-upload-id',
      status: 'ACTIVE' as const,
      identities: [],
      roles: []
    };
    const prisma = {
      user: { update: jest.fn().mockResolvedValue(updatedUser) }
    };
    const repository = new PrismaUserRepository(prisma as never);

    const result = await repository.replaceProfileImage({
      userId: 'user-id',
      imageUploadId: 'new-profile-upload-id'
    });

    expect(result.getProfileImageUploadId()).toBe('new-profile-upload-id');
    expect(prisma.user.update).toHaveBeenCalledTimes(1);
    expect(prisma.user.update).toHaveBeenCalledWith({
      where: { id: 'user-id' },
      data: { profileImageUploadId: 'new-profile-upload-id' },
      include: { identities: true, roles: true }
    });
  });

  it('maps database uniqueness conflicts to a typed association error', async () => {
    const prisma = {
      user: {
        update: jest
          .fn()
          .mockRejectedValue(Object.assign(new Error('unique'), { code: 'P2002' }))
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(
      repository.replaceProfileImage({
        userId: 'user-id',
        imageUploadId: 'profile-upload-id'
      })
    ).rejects.toBeInstanceOf(InvalidProfileImageUploadError);
  });

  it('preserves the custom relation when provider claims are resynchronized', async () => {
    const persistedUser = {
      id: 'user-id',
      email: 'player@example.test',
      name: 'Updated Provider Name',
      pictureUrl: 'https://provider.example.test/new-avatar.png',
      profileImageUploadId: 'profile-upload-id',
      identities: [{ provider: 'Google', providerSubject: 'cognito-sub' }]
    };
    const prisma = {
      user: { update: jest.fn().mockResolvedValue(persistedUser) },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue({
          userId: 'user-id',
          user: persistedUser
        })
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);
    const images = createImageRepository();
    const useCase = new SyncAuthenticatedUserUseCase(
      repository,
      new UserProfileService(images, createReadUrl())
    );

    await expect(
      useCase.execute({
        ...identity,
        name: 'Updated Provider Name',
        pictureUrl: 'https://provider.example.test/new-avatar.png'
      })
    ).resolves.toMatchObject({
      pictureUrl: 'https://read.example.test/custom-profile.jpg'
    });
    expect(prisma.user.update).toHaveBeenCalledWith({
      where: { id: 'user-id' },
      data: {
        email: 'player@example.test',
        name: 'Updated Provider Name',
        pictureUrl: 'https://provider.example.test/new-avatar.png'
      },
      include: { identities: true }
    });
  });
});
