import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { CourtNotFoundError } from '../../domain/errors/court-not-found.error';
import type {
  ICourtAdminRepository,
  ICourtAdminSnapshot
} from '../../domain/repositories/court-admin.repository';

const COURT_ADMIN_SELECT = {
  id: true,
  name: true,
  status: true,
  updatedAt: true
};

@Injectable()
export class PrismaCourtAdminRepository implements ICourtAdminRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: PrismaService
  ) {}

  async deactivateById(courtId: string): Promise<ICourtAdminSnapshot> {
    const court = await this.prisma.court.findFirst({
      where: {
        id: courtId,
        deletedAt: null
      },
      select: COURT_ADMIN_SELECT
    });

    if (court == null) {
      throw new CourtNotFoundError(courtId);
    }

    if (court.status === 'INACTIVE') {
      return this.toSnapshot(court);
    }

    const updatedCourt = await this.prisma.court.update({
      where: { id: courtId },
      data: { status: 'INACTIVE' },
      select: COURT_ADMIN_SELECT
    });

    return this.toSnapshot(updatedCourt);
  }

  async reactivateById(courtId: string): Promise<ICourtAdminSnapshot> {
    const court = await this.prisma.court.findFirst({
      where: {
        id: courtId,
        deletedAt: null
      },
      select: COURT_ADMIN_SELECT
    });

    if (court == null) {
      throw new CourtNotFoundError(courtId);
    }

    if (court.status === 'ACTIVE') {
      return this.toSnapshot(court);
    }

    const updatedCourt = await this.prisma.court.update({
      where: { id: courtId },
      data: { status: 'ACTIVE' },
      select: COURT_ADMIN_SELECT
    });

    return this.toSnapshot(updatedCourt);
  }

  private toSnapshot(court: {
    id: string;
    name: string;
    status: string;
    updatedAt: Date;
  }): ICourtAdminSnapshot {
    return {
      id: court.id,
      name: court.name,
      status: court.status,
      updatedAt: court.updatedAt.toISOString()
    };
  }
}
