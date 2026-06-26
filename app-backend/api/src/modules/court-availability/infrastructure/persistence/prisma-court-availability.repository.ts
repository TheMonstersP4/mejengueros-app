import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import type { Weekday } from '@/generated/prisma/enums';
import { CourtAvailabilityNotFoundError } from '../../domain/errors/court-availability-not-found.error';
import {
  formatTimeOnly,
  validateCourtAvailabilityInput
} from '../../domain/services/court-availability-time-range';
import type {
  ICourtAvailabilityRepository,
  ICourtAvailabilityState,
  IGetOwnedCourtAvailabilityQuery,
  ISaveOwnedCourtAvailabilityCommand
} from '../../domain/repositories/court-availability.repository';

const COGNITO_NATIVE_PROVIDER = 'Cognito';

interface ICourtAvailabilityPersistenceClient {
  court: {
    findFirst: PrismaService['court']['findFirst'];
  };
  courtAvailability: {
    create: PrismaService['courtAvailability']['create'];
    update: PrismaService['courtAvailability']['update'];
  };
}

@Injectable()
export class PrismaCourtAvailabilityRepository implements ICourtAvailabilityRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: ICourtAvailabilityPersistenceClient
  ) {}

  async getOwnedCourtAvailability(
    query: IGetOwnedCourtAvailabilityQuery
  ): Promise<ICourtAvailabilityState> {
    const court = await this.findOwnedCourtOrThrow(query);
    return this.toState(court);
  }

  async saveOwnedCourtAvailability(
    command: ISaveOwnedCourtAvailabilityCommand
  ): Promise<ICourtAvailabilityState> {
    const validated = validateCourtAvailabilityInput(command.availability);
    const court = await this.findOwnedCourtOrThrow(command);
    const dayCreates = validated.days.map((day) => ({ day }));

    if (court.availability == null) {
      await this.prisma.courtAvailability.create({
        data: {
          courtId: command.courtId,
          startTime: validated.startDate,
          endTime: validated.endDate,
          days: {
            create: dayCreates
          }
        }
      });
    } else {
      await this.prisma.courtAvailability.update({
        where: {
          courtId: command.courtId
        },
        data: {
          startTime: validated.startDate,
          endTime: validated.endDate,
          days: {
            deleteMany: {},
            create: dayCreates
          }
        }
      });
    }

    return {
      court: {
        id: court.id,
        name: court.name,
        complexId: court.complexId,
        complexName: court.complex.name
      },
      availability: {
        days: validated.days,
        startTime: validated.startTime,
        endTime: validated.endTime
      }
    };
  }

  private async findOwnedCourtOrThrow(
    query: IGetOwnedCourtAvailabilityQuery
  ): Promise<OwnedCourtRecord> {
    const court = await this.prisma.court.findFirst({
      where: {
        id: query.courtId,
        complex: {
          owner: {
            identities: {
              some: {
                provider: query.ownerIdentity.provider ?? COGNITO_NATIVE_PROVIDER,
                providerSubject: query.ownerIdentity.sub
              }
            }
          }
        }
      },
      select: {
        id: true,
        name: true,
        complexId: true,
        complex: {
          select: {
            name: true
          }
        },
        availability: {
          select: {
            id: true,
            startTime: true,
            endTime: true,
            days: {
              orderBy: {
                day: 'asc'
              },
              select: {
                day: true
              }
            }
          }
        }
      }
    });

    if (court == null) {
      throw new CourtAvailabilityNotFoundError(query.courtId);
    }

    return court as OwnedCourtRecord;
  }

  private toState(court: OwnedCourtRecord): ICourtAvailabilityState {
    return {
      court: {
        id: court.id,
        name: court.name,
        complexId: court.complexId,
        complexName: court.complex.name
      },
      availability:
        court.availability == null
          ? null
          : {
              days: court.availability.days.map(({ day }) => day),
              startTime: formatTimeOnly(court.availability.startTime),
              endTime: formatTimeOnly(court.availability.endTime)
            }
    };
  }
}

interface OwnedCourtRecord {
  id: string;
  name: string;
  complexId: string;
  complex: {
    name: string;
  };
  availability: {
    id: string;
    startTime: Date;
    endTime: Date;
    days: Array<{
      day: Weekday;
    }>;
  } | null;
}
