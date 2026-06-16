const mockConnect = jest.fn();
const mockDisconnect = jest.fn();
const mockPool = jest.fn();
const mockPrismaPg = jest.fn();
const mockPrismaClientConstructor = jest.fn();

jest.mock('pg', () => ({
  Pool: mockPool
}));

jest.mock('@prisma/adapter-pg', () => ({
  PrismaPg: mockPrismaPg
}));

jest.mock('@/generated/prisma/client', () => ({
  PrismaClient: class {
    constructor(options: unknown) {
      mockPrismaClientConstructor(options);
    }

    $connect = mockConnect;
    $disconnect = mockDisconnect;
  }
}));

import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('PrismaService', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.clearAllMocks();
    mockPool.mockImplementation((options) => ({ options }));
    mockPrismaPg.mockImplementation((pool) => ({ pool }));
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('requires DATABASE_URL before constructing a Prisma client', () => {
    delete process.env.DATABASE_URL;

    expect(() => new PrismaService()).toThrow(
      'DATABASE_URL is required to create PrismaClient.'
    );
  });

  it('creates a Prisma client with a sanitized PostgreSQL pool', () => {
    process.env.DATABASE_URL =
      'postgresql://user:pass@example.test:5432/appdb?schema=public&connection_limit=1&pool_timeout=2&connect_timeout=3';

    new PrismaService();

    expect(mockPool).toHaveBeenCalledWith({
      connectionString: 'postgresql://user:pass@example.test:5432/appdb?schema=public',
      max: 10,
      connectionTimeoutMillis: 10000,
      idleTimeoutMillis: 20000
    });
    expect(mockPrismaPg).toHaveBeenCalledWith({
      options: {
        connectionString: 'postgresql://user:pass@example.test:5432/appdb?schema=public',
        max: 10,
        connectionTimeoutMillis: 10000,
        idleTimeoutMillis: 20000
      }
    });
    expect(mockPrismaClientConstructor).toHaveBeenCalledWith({
      adapter: {
        pool: {
          options: {
            connectionString: 'postgresql://user:pass@example.test:5432/appdb?schema=public',
            max: 10,
            connectionTimeoutMillis: 10000,
            idleTimeoutMillis: 20000
          }
        }
      },
      log: ['error', 'warn']
    });
  });

  it('connects and disconnects with the Nest module lifecycle', async () => {
    process.env.DATABASE_URL = 'postgresql://user:pass@example.test/appdb';
    const service = new PrismaService();

    await service.onModuleInit();
    await service.onModuleDestroy();

    expect(mockConnect).toHaveBeenCalledTimes(1);
    expect(mockDisconnect).toHaveBeenCalledTimes(1);
  });
});
