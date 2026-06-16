import { AppEnvironment } from '@/config/runtime.constants';

process.env.NODE_ENV ??= AppEnvironment.Test;
process.env.AWS_REGION ??= 'us-east-1';
process.env.APP_S3_BUCKET_NAME ??= 'test-app-bucket';
process.env.APP_S3_REGION ??= 'us-east-1';
process.env.APP_S3_KEY_PREFIX ??= 'test';
process.env.DATABASE_URL ??= 'postgresql://user:password@localhost:5432/appdb';
process.env.COGNITO_USER_POOL_ID ??= 'us-east-1_testpool';
process.env.COGNITO_CLIENT_ID ??= 'test-client-id';
process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME ??= 'test-ws-connections';
