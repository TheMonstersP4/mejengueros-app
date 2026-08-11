import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import { FilePurpose } from '../../../files/domain/enums/file-purpose.enum';
import {
  IMAGE_UPLOAD_REPOSITORY,
  type IImageUploadRepository
} from '../../../files/domain/repositories/image-upload.repository';
import { InvalidProfileImageUploadError } from '../../domain/errors/invalid-profile-image-upload.error';
import { UserAccountInactiveError } from '../../domain/errors/user-account-inactive.error';
import {
  USER_REPOSITORY,
  type IUserRepository
} from '../../domain/repositories/user.repository';
import type { IUserProfileOutput } from '../dto/user-profile.output';
import { UserProfileService } from '../services/user-profile.service';

/**
 * Associates one confirmed profile image upload with the authenticated user.
 */
@Injectable()
export class UpdateMyProfileImageUseCase {
  constructor(
    @Inject(USER_REPOSITORY)
    private readonly userRepository: IUserRepository,
    @Inject(IMAGE_UPLOAD_REPOSITORY)
    private readonly imageUploadRepository: IImageUploadRepository,
    @Inject(UserProfileService)
    private readonly userProfileService: UserProfileService
  ) {}

  async execute(
    identity: IAuthenticatedUserOutput,
    imageUploadId: string
  ): Promise<IUserProfileOutput> {
    const user = await this.userRepository.syncAuthenticatedUser({
      cognitoSub: identity.sub,
      email: identity.email,
      emailVerified: identity.emailVerified,
      name: identity.name,
      pictureUrl: identity.pictureUrl,
      provider: identity.provider
    });
    const profile = user.toProfile();

    if (profile.status === 'INACTIVE') {
      throw new UserAccountInactiveError(identity.sub, profile.id);
    }

    const imageUpload = await this.imageUploadRepository.findById(imageUploadId);

    if (!imageUpload) {
      throw InvalidProfileImageUploadError.notFound(imageUploadId);
    }

    const upload = imageUpload.toSnapshot();

    if (upload.ownerSub !== identity.sub) {
      throw InvalidProfileImageUploadError.ownerMismatch(imageUploadId);
    }

    if (upload.purpose !== FilePurpose.ProfileImage) {
      throw InvalidProfileImageUploadError.invalidPurpose(
        imageUploadId,
        upload.purpose
      );
    }

    const updatedUser = await this.userRepository.replaceProfileImage({
      userId: profile.id,
      imageUploadId
    });

    return this.userProfileService.render(updatedUser);
  }
}
