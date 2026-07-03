import { Inject, Injectable } from '@nestjs/common';
import {
  type IUserIdentityPersistenceClient,
  type IUserRoleUpsertClient,
  upsertOwnerRole,
  upsertAuthenticatedUserIdentity
} from '../../../users/infrastructure/provisioning/demo-owner-role-provisioning';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { ComplexNotFoundForOwnerError } from '../../domain/errors/complex-not-found-for-owner.error';
import { CourtNotFoundForOwnerError } from '../../domain/errors/court-not-found-for-owner.error';
import { InvalidCourtImageUploadError } from '../../domain/errors/invalid-court-image-upload.error';
import { InvalidComplexLocationError } from '../../domain/errors/invalid-complex-location.error';
import { InvalidServiceCatalogSelectionError } from '../../domain/errors/invalid-service-catalog-selection.error';
import {
  FILE_READ_URL_PORT,
  type IFileReadUrlPort
} from '../../../files/application/ports/file-read-url.port';
import type {
  IComplexRepository,
  ICreateOwnedComplexCourtCommand,
  ICreateComplexWithFirstCourtCommand,
  ICreateComplexWithFirstCourtResult,
  ICreatedCourtSnapshot,
  IGetMyComplexHubQuery,
  IGetMyComplexHubResult,
  IMyComplexHubCourtSnapshot,
  IUpdateOwnedCourtImageCommand
} from '../../domain/repositories/complex.repository';

const MAX_COMPLEX_CREATION_TRANSACTION_ATTEMPTS = 2;
const COGNITO_NATIVE_PROVIDER = 'Cognito';

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
    findFirst: PrismaService['complex']['findFirst'];
    create: PrismaService['complex']['create'];
  };
  court: {
    findFirst: PrismaService['court']['findFirst'];
    create: PrismaService['court']['create'];
    update: PrismaService['court']['update'];
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
  complex: {
    findMany: PrismaService['complex']['findMany'];
  };
  court: {
    findFirst: PrismaService['court']['findFirst'];
  };
}

/**
 * Prisma-backed repository for atomic complex creation.
 */
@Injectable()
export class PrismaComplexRepository implements IComplexRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: IComplexPersistenceClient,
    @Inject(FILE_READ_URL_PORT)
    private readonly fileReadUrl: IFileReadUrlPort
  ) {}

  async findCourtIdByImageUploadId(imageUploadId: string): Promise<string | null> {
    const court = await this.prisma.court.findFirst({
      where: {
        imageUploadId,
        deletedAt: null
      },
      select: {
        id: true
      }
    });

    return court?.id ?? null;
  }

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

          let firstCourt;

          try {
            firstCourt = await transaction.court.create({
              data: {
                complexId: complex.id,
                name: command.firstCourt.name,
                imageUploadId: command.firstCourt.imageUploadId
              }
            });
          } catch (error) {
            if (this.isCourtImageUploadUniqueConstraint(error)) {
              throw InvalidCourtImageUploadError.alreadyAssigned(
                command.firstCourt.imageUploadId ?? ''
              );
            }

            throw error;
          }

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

  async createOwnedComplexCourt(
    command: ICreateOwnedComplexCourtCommand
  ): Promise<ICreatedCourtSnapshot> {
    return this.prisma.$transaction(async (transaction) => {
      const ownedComplex = await transaction.complex.findFirst({
        where: {
          id: command.complexId,
          deletedAt: null,
          owner: {
            identities: {
              some: {
                provider: command.ownerIdentity.provider ?? COGNITO_NATIVE_PROVIDER,
                providerSubject: command.ownerIdentity.sub
              }
            }
          }
        },
        select: {
          id: true
        }
      });

      if (ownedComplex == null) {
        throw new ComplexNotFoundForOwnerError(command.complexId);
      }

      const courtServiceIds = await this.ensureActiveServicesForScope(
        transaction,
        'court',
        'COURT',
        command.court.serviceIds
      );

      let court;

      try {
        court = await transaction.court.create({
          data: {
            complexId: ownedComplex.id,
            name: command.court.name,
            imageUploadId: command.court.imageUploadId
          }
        });
      } catch (error) {
        if (this.isCourtImageUploadUniqueConstraint(error)) {
          throw InvalidCourtImageUploadError.alreadyAssigned(command.court.imageUploadId ?? '');
        }

        throw error;
      }

      if (courtServiceIds.length > 0) {
        await transaction.courtService.createMany({
          data: courtServiceIds.map((serviceCatalogId) => ({
            courtId: court.id,
            serviceCatalogId
          }))
        });
      }

      return {
        id: court.id,
        complexId: court.complexId,
        name: court.name,
        serviceIds: courtServiceIds,
        status: court.status,
        createdAt: court.createdAt.toISOString(),
        updatedAt: court.updatedAt.toISOString()
      };
    });
  }

  async updateOwnedCourtImage(
    command: IUpdateOwnedCourtImageCommand
  ): Promise<IMyComplexHubCourtSnapshot> {
    return this.prisma.$transaction(async (transaction) => {
      const ownedCourt = await transaction.court.findFirst({
        where: {
          id: command.courtId,
          complexId: command.complexId,
          deletedAt: null,
          complex: {
            deletedAt: null,
            owner: {
              identities: {
                some: {
                  provider: command.ownerIdentity.provider ?? COGNITO_NATIVE_PROVIDER,
                  providerSubject: command.ownerIdentity.sub
                }
              }
            }
          }
        },
        select: {
          id: true
        }
      });

      if (ownedCourt == null) {
        throw new CourtNotFoundForOwnerError(command.courtId);
      }

      try {
        const updatedCourt = await transaction.court.update({
          where: { id: command.courtId },
          data: {
            imageUploadId: command.imageUploadId
          },
          select: {
            id: true,
            name: true,
            status: true,
            availability: {
              select: {
                id: true
              }
            },
            imageUpload: {
              select: {
                objectKey: true
              }
            }
          }
        });

        return this.toHubCourtSnapshot(updatedCourt);
      } catch (error) {
        if (this.isCourtImageUploadUniqueConstraint(error)) {
          throw InvalidCourtImageUploadError.alreadyAssigned(command.imageUploadId);
        }

        throw error;
      }
    });
  }

  async getMyComplexHub(query: IGetMyComplexHubQuery): Promise<IGetMyComplexHubResult> {
    const complexes = await this.prisma.complex.findMany({
      where: {
        deletedAt: null,
        owner: {
          identities: {
            some: {
              provider: query.ownerIdentity.provider ?? COGNITO_NATIVE_PROVIDER,
              providerSubject: query.ownerIdentity.sub
            }
          }
        }
      },
      orderBy: [{ createdAt: 'asc' }, { name: 'asc' }],
      select: {
        id: true,
        name: true,
        address: true,
        provinceId: true,
        cantonId: true,
        latitude: true,
        longitude: true,
        status: true,
        courts: {
          where: {
            deletedAt: null
          },
          orderBy: [{ createdAt: 'asc' }, { name: 'asc' }],
          select: {
            id: true,
            name: true,
            status: true,
            imageUpload: {
              select: {
                objectKey: true
              }
            },
            availability: {
              select: {
                id: true
              }
            }
          }
        }
      }
    });

    return {
      complexes: await Promise.all(
        complexes.map(async (complex) => ({
          id: complex.id,
          name: complex.name,
          address: complex.address,
          provinceId: complex.provinceId ?? undefined,
          cantonId: complex.cantonId ?? undefined,
          latitude: complex.latitude ?? undefined,
          longitude: complex.longitude ?? undefined,
          status: complex.status,
          courts: await Promise.all(complex.courts.map((court) => this.toHubCourtSnapshot(court)))
        }))
      )
    };
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

  private isCourtImageUploadUniqueConstraint(
    error: unknown
  ): error is { code: 'P2002'; meta?: { target?: string[] | string } } {
    if (!this.isPrismaUniqueConstraintError(error)) {
      return false;
    }

    if (typeof error !== 'object' || error == null || !('meta' in error)) {
      return false;
    }

    const target = (error as { meta?: { target?: string[] | string } }).meta?.target;

    if (Array.isArray(target)) {
      return target.includes('imageUploadId');
    }

    return target === 'imageUploadId';
  }

  private async toHubCourtSnapshot(
    court: {
      id: string;
      name: string;
      status: string;
      imageUpload?: { objectKey: string } | null;
      availability: { id: string } | null;
    }
  ): Promise<IMyComplexHubCourtSnapshot> {
    return {
      id: court.id,
      name: court.name,
      status: court.status,
      availabilityStatus: court.availability == null ? 'PENDING' : 'CONFIGURED',
      imageUrl: await this.createImageUrl(court.imageUpload?.objectKey ?? null)
    };
  }

  private async createImageUrl(objectKey: string | null): Promise<string | null> {
    if (!objectKey) {
      return null;
    }

    try {
      return await this.fileReadUrl.createReadUrl(objectKey);
    } catch {
      return null;
    }
  }
}
