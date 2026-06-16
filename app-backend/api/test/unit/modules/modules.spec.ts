import { AuthModule } from '@/modules/auth/auth.module';
import { FilesModule } from '@/modules/files/files.module';
import { HealthModule } from '@/modules/health/health.module';
import { UsersModule } from '@/modules/users/users.module';
import { AppModule } from '@/app.module';
import { PrismaModule } from '@/shared/infrastructure/database/prisma.module';

describe('Nest modules', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    process.env = {
      ...originalEnv,
      AWS_REGION: 'us-east-1',
      DATABASE_URL: 'postgresql://user:pass@example.test/db',
      COGNITO_USER_POOL_ID: 'pool',
      COGNITO_CLIENT_ID: 'client',
      WEBSOCKET_CONNECTIONS_TABLE_NAME: 'connections'
    };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('exports module classes for Nest metadata discovery', () => {
    expect(AuthModule).toBeDefined();
    expect(FilesModule).toBeDefined();
    expect(HealthModule).toBeDefined();
    expect(UsersModule).toBeDefined();
    expect(PrismaModule).toBeDefined();
  });

  it('loads the root AppModule when required environment exists', () => {
    expect(AppModule).toBeDefined();
  });
});
