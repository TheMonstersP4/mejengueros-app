import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import {
  COMPLEX_REPOSITORY,
  type IComplexRepository,
  type ICreateFirstCourtInput,
  type ICreatedCourtSnapshot
} from '../../domain/repositories/complex.repository';

@Injectable()
export class CreateCourtForOwnedComplexUseCase {
  constructor(
    @Inject(COMPLEX_REPOSITORY)
    private readonly complexRepository: IComplexRepository
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    complexId: string,
    request: ICreateFirstCourtInput
  ): Promise<ICreatedCourtSnapshot> {
    return this.complexRepository.createOwnedComplexCourt({
      ownerIdentity: {
        sub: user.sub,
        provider: user.provider
      },
      complexId,
      court: request
    });
  }
}
