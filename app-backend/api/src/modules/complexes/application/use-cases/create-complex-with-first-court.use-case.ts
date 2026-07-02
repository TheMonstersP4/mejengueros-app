import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import type { IImageUploadRepository } from '../../../files/domain/repositories/image-upload.repository';
import { IMAGE_UPLOAD_REPOSITORY } from '../../../files/domain/repositories/image-upload.repository';
import type {
  IComplexRepository,
  ICreateComplexInput,
  ICreateComplexWithFirstCourtResult,
  ICreateFirstCourtInput
} from '../../domain/repositories/complex.repository';
import { COMPLEX_REPOSITORY } from '../../domain/repositories/complex.repository';
import { validateCourtImageUpload } from '../services/validate-court-image-upload';

/**
 * Request accepted by the create-complex use case.
 */
export interface ICreateComplexWithFirstCourtRequest {
  complex: ICreateComplexInput;
  firstCourt: ICreateFirstCourtInput;
}

/**
 * Creates a complex and its first court as one application action.
 */
@Injectable()
export class CreateComplexWithFirstCourtUseCase {
  constructor(
    @Inject(COMPLEX_REPOSITORY)
    private readonly complexRepository: IComplexRepository,
    @Inject(IMAGE_UPLOAD_REPOSITORY)
    private readonly imageUploadRepository: IImageUploadRepository
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    request: ICreateComplexWithFirstCourtRequest
  ): Promise<ICreateComplexWithFirstCourtResult> {
    await validateCourtImageUpload(
      this.complexRepository,
      this.imageUploadRepository,
      user.sub,
      request.firstCourt.imageUploadId
    );

    return this.complexRepository.createComplexWithFirstCourt({
      ownerIdentity: {
        sub: user.sub,
        email: user.email,
        emailVerified: user.emailVerified,
        name: user.name,
        pictureUrl: user.pictureUrl,
        provider: user.provider
      },
      complex: request.complex,
      firstCourt: request.firstCourt
    });
  }
}
