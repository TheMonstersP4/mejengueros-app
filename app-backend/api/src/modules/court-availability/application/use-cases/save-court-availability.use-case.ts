import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import {
  COURT_AVAILABILITY_REPOSITORY,
  type ICourtAvailabilityInput,
  type ICourtAvailabilityRepository,
  type ICourtAvailabilityState
} from '../../domain/repositories/court-availability.repository';

@Injectable()
export class SaveCourtAvailabilityUseCase {
  constructor(
    @Inject(COURT_AVAILABILITY_REPOSITORY)
    private readonly repository: ICourtAvailabilityRepository
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    courtId: string,
    availability: ICourtAvailabilityInput
  ): Promise<ICourtAvailabilityState> {
    return this.repository.saveOwnedCourtAvailability({
      ownerIdentity: {
        sub: user.sub,
        provider: user.provider
      },
      courtId,
      availability
    });
  }
}
