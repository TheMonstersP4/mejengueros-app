import { InvalidProfileImageUploadError } from '@/modules/users/domain/errors/invalid-profile-image-upload.error';
import { PrismaUserRepository } from '@/modules/users/infrastructure/persistence/prisma-user.repository';

describe('user profile image association', () => {
  it('replaces the relation with one atomic user update', async () => {
    const updatedUser = {
      id: 'user-id',
      email: 'player@example.test',
      pictureUrl: 'https://provider.example.test/avatar.png',
      profileImageUploadId: 'new-profile-upload-id',
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
});
