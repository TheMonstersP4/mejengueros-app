import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import type {
  IComplexRepository,
  ICreateComplexInput,
  ICreateComplexWithFirstCourtResult,
  ICreateFirstCourtInput
} from '../../domain/repositories/complex.repository';
import { COMPLEX_REPOSITORY } from '../../domain/repositories/complex.repository';

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
    private readonly complexRepository: IComplexRepository
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    request: ICreateComplexWithFirstCourtRequest
  ): Promise<ICreateComplexWithFirstCourtResult> {
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
