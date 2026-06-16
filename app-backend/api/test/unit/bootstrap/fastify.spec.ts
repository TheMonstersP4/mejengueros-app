const mockCreate = jest.fn();
const mockLoadDatabaseUrlFromSecret = jest.fn();

jest.mock('@nestjs/core', () => ({
  NestFactory: {
    create: mockCreate
  }
}));

jest.mock('@nestjs/platform-fastify', () => ({
  FastifyAdapter: jest.fn().mockImplementation((options) => ({ options }))
}));

jest.mock('@/app.module', () => ({
  AppModule: class AppModule {}
}));

jest.mock('@/bootstrap/database-secret', () => ({
  loadDatabaseUrlFromSecret: mockLoadDatabaseUrlFromSecret
}));

import { Logger } from 'nestjs-pino';
import { ConfigService } from '@nestjs/config';
import { createFastifyApp } from '@/bootstrap/fastify';

describe('createFastifyApp', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockLoadDatabaseUrlFromSecret.mockResolvedValue(undefined);
  });

  it('creates a buffered Fastify Nest application', async () => {
    const app = {
      get: jest.fn((token: unknown) =>
        token === Logger
          ? 'logger-instance'
          : {
              get: jest.fn().mockReturnValue([])
            }
      ),
      useLogger: jest.fn(),
      setGlobalPrefix: jest.fn(),
      enableCors: jest.fn()
    };
    mockCreate.mockResolvedValue(app);

    await expect(createFastifyApp()).resolves.toBe(app);

    expect(mockLoadDatabaseUrlFromSecret).toHaveBeenCalledTimes(1);
    expect(mockCreate).toHaveBeenCalledWith(
      expect.any(Function),
      { options: { logger: false } },
      { bufferLogs: true }
    );
    expect(app.get).toHaveBeenCalledWith(Logger);
    expect(app.get).toHaveBeenCalledWith(ConfigService);
    expect(app.useLogger).toHaveBeenCalledWith('logger-instance');
    expect(app.setGlobalPrefix).toHaveBeenCalledWith('v1');
    expect(app.enableCors).not.toHaveBeenCalled();
  });

  it('enables CORS when allowed origins are configured', async () => {
    const configService = {
      get: jest.fn().mockReturnValue(['http://localhost:3000'])
    };
    const app = {
      get: jest.fn((token: unknown) =>
        token === Logger ? 'logger-instance' : configService
      ),
      useLogger: jest.fn(),
      setGlobalPrefix: jest.fn(),
      enableCors: jest.fn()
    };
    mockCreate.mockResolvedValue(app);

    await createFastifyApp();

    expect(app.enableCors).toHaveBeenCalledWith({
      origin: ['http://localhost:3000'],
      methods: ['GET', 'POST', 'OPTIONS'],
      allowedHeaders: ['Authorization', 'Content-Type'],
      maxAge: 300
    });
  });
});
