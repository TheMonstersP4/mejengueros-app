import { Inject, Injectable } from '@nestjs/common';
import {
  grantDemoOwnerRoleIfEligible,
  hasPersistedOwnerRole,
  upsertAuthenticatedUserIdentity
} from '../../../users/infrastructure/provisioning/demo-owner-role-provisioning';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { OwnerRoleRequiredError } from '../../domain/errors/owner-role-required.error';
import type {
  IComplexRepository,
  ICreateComplexWithFirstCourtCommand,
  ICreateComplexWithFirstCourtResult
} from '../../domain/repositories/complex.repository';

/**
 * Prisma-backed repository for atomic complex creation.
 */
@Injectable()
export class PrismaComplexRepository implements IComplexRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: PrismaService
  ) {}

  async createComplexWithFirstCourt(
    command: ICreateComplexWithFirstCourtCommand
  ): Promise<ICreateComplexWithFirstCourtResult> {
    const ownerIdentity = {
      cognitoSub: command.ownerIdentity.sub,
      email: command.ownerIdentity.email,
      emailVerified: command.ownerIdentity.emailVerified,
      name: command.ownerIdentity.name,
      pictureUrl: command.ownerIdentity.pictureUrl,
      provider: command.ownerIdentity.provider
    };

    return this.prisma.$transaction(async (transaction) => {
      const owner = await upsertAuthenticatedUserIdentity(transaction, ownerIdentity, {
        selectIdOnly: true
      });

      await grantDemoOwnerRoleIfEligible(transaction, owner.id, ownerIdentity);
      const ownerRole = await hasPersistedOwnerRole(transaction, owner.id);

      if (!ownerRole) {
        throw new OwnerRoleRequiredError(command.ownerIdentity.sub);
      }

      const complex = await transaction.complex.create({
        data: {
          ownerId: owner.id,
          name: command.complex.name,
          address: command.complex.address
        }
      });

      const firstCourt = await transaction.court.create({
        data: {
          complexId: complex.id,
          name: command.firstCourt.name
        }
      });

      return {
        complex: {
          id: complex.id,
          name: complex.name,
          address: complex.address,
          status: complex.status,
          createdAt: complex.createdAt.toISOString(),
          updatedAt: complex.updatedAt.toISOString()
        },
        firstCourt: {
          id: firstCourt.id,
          complexId: firstCourt.complexId,
          name: firstCourt.name,
          status: firstCourt.status,
          createdAt: firstCourt.createdAt.toISOString(),
          updatedAt: firstCourt.updatedAt.toISOString()
        }
      };
    });
  }
}
