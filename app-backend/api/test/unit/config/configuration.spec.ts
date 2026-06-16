import { appConfig } from '@/config/app.config';
import { authConfig } from '@/config/auth.config';
import { configuration } from '@/config/configuration';
import { databaseConfig } from '@/config/database.config';
import { validateEnv } from '@/config/env.validation';
import { loggerAppConfig, loggerConfig } from '@/config/logger.config';
import { storageConfig } from '@/config/storage.config';
import {
  AppEnvironment,
  AppLogLevel,
  DEFAULT_APP_ENVIRONMENT,
  DEFAULT_HTTP_PORT,
  DEFAULT_LOG_LEVEL
} from '@/config/runtime.constants';
import { websocketConfig } from '@/config/websocket.config';

describe('configuration', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('loads defaults when optional environment variables are absent', () => {
    delete process.env.NODE_ENV;
    delete process.env.PORT;
    delete process.env.LOG_LEVEL;
    delete process.env.AWS_REGION;
    delete process.env.APP_CORS_ALLOWED_ORIGINS;
    delete process.env.APP_S3_BUCKET_NAME;
    delete process.env.APP_S3_REGION;
    delete process.env.APP_S3_KEY_PREFIX;
    delete process.env.APP_S3_UPLOAD_URL_TTL_SECONDS;
    delete process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES;
    delete process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES;
    delete process.env.COGNITO_USER_POOL_ID;
    delete process.env.COGNITO_CLIENT_ID;
    delete process.env.COGNITO_TOKEN_USE;
    delete process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME;
    delete process.env.WEBSOCKET_CONNECTION_TTL_SECONDS;

    expect(appConfig()).toEqual({
      environment: DEFAULT_APP_ENVIRONMENT,
      port: DEFAULT_HTTP_PORT,
      corsAllowedOrigins: []
    });
    expect(loggerAppConfig()).toEqual({ level: DEFAULT_LOG_LEVEL });
    expect(loggerConfig().pinoHttp).toEqual(
      expect.objectContaining({ level: DEFAULT_LOG_LEVEL })
    );
    expect(websocketConfig()).toEqual({
      connectionsTableName: '',
      connectionTtlSeconds: 86400
    });
    expect(authConfig()).toEqual({
      awsRegion: 'us-east-2',
      cognitoUserPoolId: '',
      cognitoClientId: '',
      cognitoTokenUse: 'id'
    });
    expect(storageConfig()).toEqual({
      bucketName: '',
      region: 'us-east-2',
      keyPrefix: 'uploads',
      uploadUrlTtlSeconds: 300,
      profileImageMaxBytes: 5242880,
      allowedImageMimeTypes: ['image/jpeg', 'image/png', 'image/webp']
    });
  });

  it('loads configured application sections from the environment', () => {
    process.env.NODE_ENV = AppEnvironment.Test;
    process.env.PORT = '4444';
    process.env.LOG_LEVEL = AppLogLevel.Debug;
    process.env.AWS_REGION = 'us-west-2';
    process.env.APP_CORS_ALLOWED_ORIGINS =
      'http://localhost:3000,https://mejengueros-dev.scvdev.net';
    process.env.APP_S3_BUCKET_NAME = 'app-bucket';
    process.env.APP_S3_REGION = 'us-west-1';
    process.env.APP_S3_KEY_PREFIX = 'dev';
    process.env.APP_S3_UPLOAD_URL_TTL_SECONDS = '120';
    process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES = '1024';
    process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES = 'image/jpeg,image/png';
    process.env.DATABASE_URL = 'postgresql://user:pass@example.test/db';
    process.env.DATABASE_SECRET_ARN = 'arn:aws:secretsmanager:us-west-2:123:secret:db';
    process.env.COGNITO_USER_POOL_ID = 'pool';
    process.env.COGNITO_CLIENT_ID = 'client';
    process.env.COGNITO_TOKEN_USE = 'access';
    process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME = 'connections';
    process.env.WEBSOCKET_CONNECTION_TTL_SECONDS = '30';

    expect(configuration()).toEqual({
      app: {
        environment: AppEnvironment.Test,
        port: 4444,
        corsAllowedOrigins: [
          'http://localhost:3000',
          'https://mejengueros-dev.scvdev.net'
        ]
      },
      auth: {
        awsRegion: 'us-west-2',
        cognitoUserPoolId: 'pool',
        cognitoClientId: 'client',
        cognitoTokenUse: 'access'
      },
      database: {
        url: 'postgresql://user:pass@example.test/db',
        secretArn: 'arn:aws:secretsmanager:us-west-2:123:secret:db'
      },
      logger: { level: AppLogLevel.Debug },
      storage: {
        bucketName: 'app-bucket',
        region: 'us-west-1',
        keyPrefix: 'dev',
        uploadUrlTtlSeconds: 120,
        profileImageMaxBytes: 1024,
        allowedImageMimeTypes: ['image/jpeg', 'image/png']
      },
      websocket: {
        connectionsTableName: 'connections',
        connectionTtlSeconds: 30
      }
    });
  });

  it('returns empty required values before validation rejects them', () => {
    delete process.env.DATABASE_URL;

    delete process.env.DATABASE_SECRET_ARN;

    expect(databaseConfig()).toEqual({ url: '', secretArn: '' });
  });

  it('validates required environment variables and coerces defaults', () => {
    const parsed = validateEnv({
      AWS_REGION: 'us-east-1',
      APP_S3_BUCKET_NAME: 'app-bucket',
      COGNITO_USER_POOL_ID: 'pool',
      COGNITO_CLIENT_ID: 'client',
      WEBSOCKET_CONNECTIONS_TABLE_NAME: 'connections',
      ERROR_DOCUMENTATION_BASE_URL: ''
    });

    expect(parsed).toEqual(
      expect.objectContaining({
        NODE_ENV: DEFAULT_APP_ENVIRONMENT,
        PORT: DEFAULT_HTTP_PORT,
        LOG_LEVEL: DEFAULT_LOG_LEVEL,
        APP_CORS_ALLOWED_ORIGINS: '',
        APP_S3_KEY_PREFIX: 'uploads',
        APP_S3_UPLOAD_URL_TTL_SECONDS: 300,
        APP_S3_PROFILE_IMAGE_MAX_BYTES: 5242880,
        APP_S3_ALLOWED_IMAGE_MIME_TYPES: 'image/jpeg,image/png,image/webp',
        COGNITO_TOKEN_USE: 'id',
        WEBSOCKET_CONNECTION_TTL_SECONDS: 86400
      })
    );
    expect(parsed.ERROR_DOCUMENTATION_BASE_URL).toBeUndefined();
    expect(parsed.DATABASE_URL).toBeUndefined();
    expect(parsed.DATABASE_SECRET_ARN).toBeUndefined();
  });

  it('rejects invalid environment values', () => {
    expect(() =>
      validateEnv({
        AWS_REGION: '',
        APP_S3_BUCKET_NAME: '',
        APP_S3_REGION: '',
        APP_S3_KEY_PREFIX: '',
        APP_S3_UPLOAD_URL_TTL_SECONDS: '901',
        APP_S3_PROFILE_IMAGE_MAX_BYTES: '8388609',
        APP_S3_ALLOWED_IMAGE_MIME_TYPES: '',
        NODE_ENV: 'staging',
        DATABASE_URL: '',
        COGNITO_USER_POOL_ID: '',
        COGNITO_CLIENT_ID: '',
        LOG_LEVEL: 'verbose',
        COGNITO_TOKEN_USE: 'refresh',
        WEBSOCKET_CONNECTIONS_TABLE_NAME: '',
        WEBSOCKET_CONNECTION_TTL_SECONDS: '0'
      })
    ).toThrow();
  });

  it('uses pino-pretty outside production and disables transport in production', () => {
    process.env.LOG_LEVEL = AppLogLevel.Trace;
    process.env.NODE_ENV = AppEnvironment.Development;

    expect(loggerConfig()).toEqual({
      pinoHttp: expect.objectContaining({
        level: AppLogLevel.Trace,
        redact: ['req.headers.authorization', 'req.headers.cookie'],
        transport: {
          target: 'pino-pretty',
          options: {
            singleLine: true,
            colorize: true
          }
        }
      })
    });

    process.env.NODE_ENV = AppEnvironment.Production;
    delete process.env.LOG_LEVEL;

    expect(loggerConfig()).toEqual({
      pinoHttp: expect.objectContaining({
        level: DEFAULT_LOG_LEVEL,
        transport: undefined
      })
    });
  });
});
