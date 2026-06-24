import { Inject, Injectable } from '@nestjs/common';
import {
  upsertOwnerRole,
  upsertAuthenticatedUserIdentity
} from '../../../users/infrastructure/provisioning/demo-owner-role-provisioning';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import type {
  IComplexRepository,
  ICreateComplexWithFirstCourtCommand,
  ICreateComplexWithFirstCourtResult
} from '../../domain/repositories/complex.repository';

interface IComplexPersistenceTransactionClient {
  user: {
    findUnique: PrismaService['user']['findUnique'];
    update: PrismaService['user']['update'];
    create: PrismaService['user']['create'];
  };
  userIdentity: {
    findUnique: PrismaService['userIdentity']['findUnique'];
  };
  userRole: {
    upsert: PrismaService['userRole']['upsert'];
  };
  complex: {
    create: PrismaService['complex']['create'];
  };
  court: {
    create: PrismaService['court']['create'];
  };
}

interface IComplexPersistenceClient {
  $transaction<TResult>(
    callback: (transaction: IComplexPersistenceTransactionClient) => Promise<TResult>
  ): Promise<TResult>;
}

/**
 * Prisma-backed repository for atomic complex creation.
 */
@Injectable()
export class PrismaComplexRepository implements IComplexRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: IComplexPersistenceClient
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

      await upsertOwnerRole(transaction, owner.id);

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
