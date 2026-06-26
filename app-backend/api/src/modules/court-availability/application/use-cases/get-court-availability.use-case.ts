import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import {
  COURT_AVAILABILITY_REPOSITORY,
  type ICourtAvailabilityRepository,
  type ICourtAvailabilityState
} from '../../domain/repositories/court-availability.repository';

@Injectable()
export class GetCourtAvailabilityUseCase {
  constructor(
    @Inject(COURT_AVAILABILITY_REPOSITORY)
    private readonly repository: ICourtAvailabilityRepository
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    courtId: string
  ): Promise<ICourtAvailabilityState> {
    return this.repository.getOwnedCourtAvailability({
      ownerIdentity: {
        sub: user.sub,
        provider: user.provider
      },
      courtId
    });
  }
}
