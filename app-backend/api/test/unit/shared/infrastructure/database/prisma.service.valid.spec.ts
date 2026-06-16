const mockConnect = jest.fn();
const mockDisconnect = jest.fn();
const mockPrismaClient = jest.fn();
const mockPool = jest.fn((options) => ({ options }));
const mockPrismaPg = jest.fn((pool) => ({ pool }));

jest.mock('pg', () => ({
  Pool: mockPool
}));

jest.mock('@prisma/adapter-pg', () => ({
  PrismaPg: mockPrismaPg
}));

jest.mock('@/generated/prisma/client', () => ({
  PrismaClient: class PrismaClient {
    constructor(options: unknown) {
      mockPrismaClient(options);
    }

    $connect = mockConnect;
    $disconnect = mockDisconnect;
  }
}));

import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('PrismaService with a configured database URL', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.clearAllMocks();
    process.env = {
      ...originalEnv,
      DATABASE_URL:
        'postgresql://user:pass@example.test/db?connection_limit=1&pool_timeout=2&connect_timeout=3&schema=public'
    };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('creates Prisma with a pg adapter and sanitized connection string', async () => {
    const service = new PrismaService();

    expect(mockPool).toHaveBeenCalledWith({
      connectionString:
        'postgresql://user:pass@example.test/db?schema=public',
      max: 10,
      connectionTimeoutMillis: 10000,
      idleTimeoutMillis: 20000
    });
    expect(mockPrismaPg).toHaveBeenCalledWith({
      options: expect.objectContaining({
        connectionString:
          'postgresql://user:pass@example.test/db?schema=public'
      })
    });
    expect(mockPrismaClient).toHaveBeenCalledWith({
      adapter: {
        pool: {
          options: expect.objectContaining({
            connectionString:
              'postgresql://user:pass@example.test/db?schema=public'
          })
        }
      },
      log: ['error', 'warn']
    });

    await service.onModuleInit();
    await service.onModuleDestroy();

    expect(mockConnect).toHaveBeenCalledTimes(1);
    expect(mockDisconnect).toHaveBeenCalledTimes(1);
  });
});
