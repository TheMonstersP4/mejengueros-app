import { Inject, Injectable } from '@nestjs/common';
import {
  FILE_READ_URL_PORT,
  type IFileReadUrlPort
} from '../../../files/application/ports/file-read-url.port';
import {
  IMAGE_UPLOAD_REPOSITORY,
  type IImageUploadRepository
} from '../../../files/domain/repositories/image-upload.repository';
import type { UserEntity } from '../../domain/entities/user.entity';
import type { IUserProfileOutput } from '../dto/user-profile.output';

/**
 * Builds effective user profiles with short-lived custom image URLs.
 */
@Injectable()
export class UserProfileService {
  constructor(
    @Inject(IMAGE_UPLOAD_REPOSITORY)
    private readonly imageUploadRepository: IImageUploadRepository,
    @Inject(FILE_READ_URL_PORT)
    private readonly fileReadUrl: IFileReadUrlPort
  ) {}

  async render(user: UserEntity): Promise<IUserProfileOutput> {
    const profile = user.toProfile();
    const imageUploadId = user.getProfileImageUploadId();

    if (!imageUploadId) {
      return profile;
    }

    const imageUpload = await this.imageUploadRepository.findById(imageUploadId);

    if (!imageUpload) {
      return profile;
    }

    return {
      ...profile,
      pictureUrl: await this.fileReadUrl.createReadUrl(
        imageUpload.toSnapshot().objectKey
      )
    };
  }
}
