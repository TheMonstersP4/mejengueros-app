import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import type { IImageUploadRepository } from '../../../files/domain/repositories/image-upload.repository';
import { IMAGE_UPLOAD_REPOSITORY } from '../../../files/domain/repositories/image-upload.repository';
import {
  COMPLEX_REPOSITORY,
  type IComplexRepository,
  type IMyComplexHubCourtSnapshot
} from '../../domain/repositories/complex.repository';
import { validateCourtImageUpload } from '../services/validate-court-image-upload';

export interface IUpdateOwnedCourtImageRequest {
  imageUploadId: string;
}

@Injectable()
export class UpdateOwnedCourtImageUseCase {
  constructor(
    @Inject(COMPLEX_REPOSITORY)
    private readonly complexRepository: IComplexRepository,
    @Inject(IMAGE_UPLOAD_REPOSITORY)
    private readonly imageUploadRepository: IImageUploadRepository
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    complexId: string,
    courtId: string,
    request: IUpdateOwnedCourtImageRequest
  ): Promise<IMyComplexHubCourtSnapshot> {
    await validateCourtImageUpload(
      this.complexRepository,
      this.imageUploadRepository,
      user.sub,
      request.imageUploadId,
      courtId
    );

    return this.complexRepository.updateOwnedCourtImage({
      ownerIdentity: {
        sub: user.sub,
        provider: user.provider
      },
      complexId,
      courtId,
      imageUploadId: request.imageUploadId
    });
  }
}
