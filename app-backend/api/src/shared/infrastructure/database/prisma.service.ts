import type { OnModuleDestroy, OnModuleInit } from '@nestjs/common';
import { Injectable } from '@nestjs/common';
import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import { PrismaClient } from '../../../generated/prisma/client';

const DATABASE_CONNECTION_LIMIT = 10;
const DATABASE_CONNECT_TIMEOUT_MS = 10_000;
const DATABASE_IDLE_TIMEOUT_MS = 20_000;

/**
 * Shared Prisma client for NestJS providers.
 *
 * @remarks
 * Prisma ORM 7 uses driver adapters for runtime database connections. This
 * service owns the PostgreSQL pool and exposes the generated Prisma Client
 * through Nest dependency injection.
 */
@Injectable()
export class PrismaService
  extends PrismaClient
  implements OnModuleInit, OnModuleDestroy
{
  /**
   * Creates a Prisma client backed by the PostgreSQL driver adapter.
   *
   * @throws Error when `DATABASE_URL` is missing at runtime.
   */
  constructor() {
    const databaseUrl = process.env.DATABASE_URL;

    if (!databaseUrl) {
      throw new Error('DATABASE_URL is required to create PrismaClient.');
    }

    const pool = new Pool({
      connectionString: sanitizeConnectionString(databaseUrl),
      max: DATABASE_CONNECTION_LIMIT,
      connectionTimeoutMillis: DATABASE_CONNECT_TIMEOUT_MS,
      idleTimeoutMillis: DATABASE_IDLE_TIMEOUT_MS
    });

    super({
      adapter: new PrismaPg(pool),
      log: ['error', 'warn']
    });
  }

  async onModuleInit(): Promise<void> {
    await this.$connect();
  }

  async onModuleDestroy(): Promise<void> {
    await this.$disconnect();
  }
}

/**
 * Removes legacy Prisma connection-pool query parameters before passing the URL
 * to the `pg` driver.
 *
 * @param rawConnectionString - Database URL received from environment config.
 * @returns Database URL accepted by the PostgreSQL driver.
 */
function sanitizeConnectionString(rawConnectionString: string): string {
  const connectionUrl = new URL(rawConnectionString);

  connectionUrl.searchParams.delete('connection_limit');
  connectionUrl.searchParams.delete('pool_timeout');
  connectionUrl.searchParams.delete('connect_timeout');

  return connectionUrl.toString();
}
