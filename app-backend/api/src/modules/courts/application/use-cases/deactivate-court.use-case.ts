import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '@/modules/auth/application/dto/authenticated-user.output';
import { AdminRoleRequiredError } from '../../domain/errors/admin-role-required.error';
import {
  COURT_ADMIN_REPOSITORY,
  type ICourtAdminRepository,
  type ICourtAdminSnapshot
} from '../../domain/repositories/court-admin.repository';

@Injectable()
export class DeactivateCourtUseCase {
  constructor(
    @Inject(COURT_ADMIN_REPOSITORY)
    private readonly courtAdminRepository: ICourtAdminRepository
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    courtId: string
  ): Promise<ICourtAdminSnapshot> {
    if (!user.groups.some((group) => group.toLowerCase() === 'admin')) {
      throw new AdminRoleRequiredError(user.sub);
    }

    return this.courtAdminRepository.deactivateById(courtId);
  }
}
