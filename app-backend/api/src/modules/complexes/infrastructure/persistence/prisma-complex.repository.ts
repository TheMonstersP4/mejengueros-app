import { Inject, Injectable } from '@nestjs/common';
import {
  type IUserIdentityPersistenceClient,
  type IUserRoleUpsertClient,
  upsertOwnerRole,
  upsertAuthenticatedUserIdentity
} from '../../../users/infrastructure/provisioning/demo-owner-role-provisioning';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { InvalidComplexLocationError } from '../../domain/errors/invalid-complex-location.error';
import { InvalidServiceCatalogSelectionError } from '../../domain/errors/invalid-service-catalog-selection.error';
import type {
  IComplexRepository,
  ICreateComplexWithFirstCourtCommand,
  ICreateComplexWithFirstCourtResult
} from '../../domain/repositories/complex.repository';

const MAX_COMPLEX_CREATION_TRANSACTION_ATTEMPTS = 2;

type IServiceCatalogScope = 'COMPLEX' | 'COURT';
type IServiceCatalogTarget = 'complex' | 'court';

interface IComplexPersistenceTransactionClient
  extends IUserIdentityPersistenceClient<{ id: string }>,
    IUserRoleUpsertClient {
  province: {
    findUnique: PrismaService['province']['findUnique'];
  };
  canton: {
    findFirst: PrismaService['canton']['findFirst'];
  };
  serviceCatalog: {
    findMany: PrismaService['serviceCatalog']['findMany'];
  };
  complex: {
    create: PrismaService['complex']['create'];
  };
  court: {
    create: PrismaService['court']['create'];
  };
  complexService: {
    createMany: PrismaService['complexService']['createMany'];
  };
  courtService: {
    createMany: PrismaService['courtService']['createMany'];
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

    let lastUniqueConstraintError: unknown;

    for (let attempt = 0; attempt < MAX_COMPLEX_CREATION_TRANSACTION_ATTEMPTS; attempt += 1) {
      try {
        return await this.prisma.$transaction(async (transaction) => {
          await this.ensureProvinceExists(transaction, command.complex.provinceId);
          await this.ensureCantonBelongsToProvince(
            transaction,
            command.complex.provinceId,
            command.complex.cantonId
          );

          const complexServiceIds = await this.ensureActiveServicesForScope(
            transaction,
            'complex',
            'COMPLEX',
            command.complex.serviceIds
          );
          const courtServiceIds = await this.ensureActiveServicesForScope(
            transaction,
            'court',
            'COURT',
            command.firstCourt.serviceIds
          );

          const owner = await upsertAuthenticatedUserIdentity(transaction, ownerIdentity, {
            selectIdOnly: true,
            retryOnUniqueConstraint: false
          });

          await upsertOwnerRole(transaction, owner.id);

          const complex = await transaction.complex.create({
            data: {
              ownerId: owner.id,
              name: command.complex.name,
              provinceId: command.complex.provinceId,
              cantonId: command.complex.cantonId,
              address: command.complex.address,
              latitude: command.complex.latitude,
              longitude: command.complex.longitude,
              isPublished: true
            }
          });

          if (complexServiceIds.length > 0) {
            await transaction.complexService.createMany({
              data: complexServiceIds.map((serviceCatalogId) => ({
                complexId: complex.id,
                serviceCatalogId
              }))
            });
          }

          const firstCourt = await transaction.court.create({
            data: {
              complexId: complex.id,
              name: command.firstCourt.name,
              isPublished: true
            }
          });

          if (courtServiceIds.length > 0) {
            await transaction.courtService.createMany({
              data: courtServiceIds.map((serviceCatalogId) => ({
                courtId: firstCourt.id,
                serviceCatalogId
              }))
            });
          }

          return {
            complex: {
              id: complex.id,
              name: complex.name,
              provinceId: complex.provinceId ?? command.complex.provinceId,
              cantonId: complex.cantonId ?? command.complex.cantonId,
              address: complex.address,
              latitude: complex.latitude ?? undefined,
              longitude: complex.longitude ?? undefined,
              serviceIds: complexServiceIds,
              status: complex.status,
              createdAt: complex.createdAt.toISOString(),
              updatedAt: complex.updatedAt.toISOString()
            },
            firstCourt: {
              id: firstCourt.id,
              complexId: firstCourt.complexId,
              name: firstCourt.name,
              serviceIds: courtServiceIds,
              status: firstCourt.status,
              createdAt: firstCourt.createdAt.toISOString(),
              updatedAt: firstCourt.updatedAt.toISOString()
            }
          };
        });
      } catch (error) {
        if (!this.isPrismaUniqueConstraintError(error)) {
          throw error;
        }

        lastUniqueConstraintError = error;
      }
    }

    throw lastUniqueConstraintError;
  }

  private async ensureProvinceExists(
    transaction: IComplexPersistenceTransactionClient,
    provinceId: string
  ): Promise<void> {
    const province = await transaction.province.findUnique({
      where: { id: provinceId },
      select: { id: true }
    });

    if (!province) {
      throw InvalidComplexLocationError.invalidProvince(provinceId);
    }
  }

  private async ensureCantonBelongsToProvince(
    transaction: IComplexPersistenceTransactionClient,
    provinceId: string,
    cantonId: string
  ): Promise<void> {
    const canton = await transaction.canton.findFirst({
      where: {
        id: cantonId,
        provinceId
      },
      select: { id: true }
    });

    if (!canton) {
      throw InvalidComplexLocationError.invalidCanton(provinceId, cantonId);
    }
  }

  private async ensureActiveServicesForScope(
    transaction: IComplexPersistenceTransactionClient,
    target: IServiceCatalogTarget,
    scope: IServiceCatalogScope,
    serviceIds: string[]
  ): Promise<string[]> {
    const uniqueServiceIds = Array.from(new Set(serviceIds));

    if (uniqueServiceIds.length === 0) {
      return [];
    }

    const activeServices = await transaction.serviceCatalog.findMany({
      where: {
        id: {
          in: uniqueServiceIds
        },
        scope,
        isActive: true
      },
      select: {
        id: true
      }
    });

    if (activeServices.length !== uniqueServiceIds.length) {
      throw new InvalidServiceCatalogSelectionError(target, scope, uniqueServiceIds);
    }

    return uniqueServiceIds;
  }

  private isPrismaUniqueConstraintError(error: unknown): error is { code: 'P2002' } {
    return (
      typeof error === 'object' &&
      error !== null &&
      'code' in error &&
      error.code === 'P2002'
    );
  }
}
