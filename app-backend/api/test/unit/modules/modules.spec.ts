import { AuthModule } from '@/modules/auth/auth.module';
import { ComplexesModule } from '@/modules/complexes/complexes.module';
import { CourtsModule } from '@/modules/courts/courts.module';
import { CourtAvailabilityModule } from '@/modules/court-availability/court-availability.module';
import { FilesModule } from '@/modules/files/files.module';
import { HealthModule } from '@/modules/health/health.module';
import { LocationsModule } from '@/modules/locations/locations.module';
import { ReservationsModule } from '@/modules/reservations/reservations.module';
import { ReviewsModule } from '@/modules/reviews/reviews.module';
import { ServiceCatalogModule } from '@/modules/service-catalog/service-catalog.module';
import { UsersModule } from '@/modules/users/users.module';
import { PrismaModule } from '@/shared/infrastructure/database/prisma.module';

describe('Nest modules', () => {
  const originalEnv = process.env;
  const envFixturePath = `${process.cwd()}/test/fixtures/app-module.with-db.env`;

  beforeEach(() => {
    jest.resetModules();
    process.env = {
      ...originalEnv,
      AWS_REGION: 'us-east-1',
      DATABASE_URL: 'postgresql://user:pass@example.test/db',
      COGNITO_USER_POOL_ID: 'pool',
      COGNITO_CLIENT_ID: 'client',
      WEBSOCKET_CONNECTIONS_TABLE_NAME: 'connections'
    };
  });

  afterEach(() => {
    jest.resetModules();
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('exports module classes for Nest metadata discovery', () => {
    expect(AuthModule).toBeDefined();
    expect(ComplexesModule).toBeDefined();
    expect(CourtsModule).toBeDefined();
    expect(CourtAvailabilityModule).toBeDefined();
    expect(FilesModule).toBeDefined();
    expect(HealthModule).toBeDefined();
    expect(LocationsModule).toBeDefined();
    expect(ReservationsModule).toBeDefined();
    expect(ReviewsModule).toBeDefined();
    expect(ServiceCatalogModule).toBeDefined();
    expect(UsersModule).toBeDefined();
    expect(PrismaModule).toBeDefined();
  });

  it('loads the root AppModule when required environment exists', async () => {
    const { AppModule } = await import('@/app.module');

    expect(AppModule).toBeDefined();
  });

  it('loads the root AppModule from a dotenv file before module metadata reads DATABASE_URL', async () => {
    delete process.env.AWS_REGION;
    delete process.env.DATABASE_URL;
    delete process.env.COGNITO_USER_POOL_ID;
    delete process.env.COGNITO_CLIENT_ID;
    delete process.env.APP_S3_BUCKET_NAME;
    delete process.env.APP_S3_KEY_PREFIX;
    delete process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES;
    delete process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES;
    delete process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME;
    process.env.DOTENV_CONFIG_PATH = envFixturePath;

    const { AppModule } = await import('@/app.module');

    expect(AppModule).toBeDefined();
  });

  it('loads the root AppModule without DATABASE_URL for non database scenarios', async () => {
    delete process.env.DATABASE_URL;

    const { AppModule } = await import('@/app.module');

    expect(AppModule).toBeDefined();
  });
});
