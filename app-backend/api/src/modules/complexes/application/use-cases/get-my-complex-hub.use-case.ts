import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import {
  COMPLEX_REPOSITORY,
  type IComplexRepository,
  type IGetMyComplexHubResult
} from '../../domain/repositories/complex.repository';

@Injectable()
export class GetMyComplexHubUseCase {
  constructor(
    @Inject(COMPLEX_REPOSITORY)
    private readonly complexRepository: IComplexRepository
  ) {}

  async execute(user: IAuthenticatedUserOutput): Promise<IGetMyComplexHubResult> {
    return this.complexRepository.getMyComplexHub({
      ownerIdentity: {
        sub: user.sub,
        provider: user.provider
      }
    });
  }
}
