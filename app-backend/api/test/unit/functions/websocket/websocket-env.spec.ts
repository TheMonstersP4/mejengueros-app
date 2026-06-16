import { loadWebSocketFunctionEnv } from '@/functions/websocket/websocket-env';

describe('loadWebSocketFunctionEnv', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    process.env = {
      ...originalEnv,
      AWS_REGION: 'us-east-1',
      COGNITO_USER_POOL_ID: 'pool',
      COGNITO_CLIENT_ID: 'client',
      WEBSOCKET_CONNECTIONS_TABLE_NAME: 'connections'
    };
    delete process.env.COGNITO_TOKEN_USE;
    delete process.env.WEBSOCKET_CONNECTION_TTL_SECONDS;
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('loads required values and defaults', () => {
    expect(loadWebSocketFunctionEnv()).toEqual({
      awsRegion: 'us-east-1',
      cognitoUserPoolId: 'pool',
      cognitoClientId: 'client',
      cognitoTokenUse: 'id',
      connectionsTableName: 'connections',
      connectionTtlSeconds: 86400
    });
  });

  it('loads configured token use and ttl', () => {
    process.env.COGNITO_TOKEN_USE = 'access';
    process.env.WEBSOCKET_CONNECTION_TTL_SECONDS = '120';

    expect(loadWebSocketFunctionEnv()).toEqual(
      expect.objectContaining({
        cognitoTokenUse: 'access',
        connectionTtlSeconds: 120
      })
    );
  });

  it('throws when a required variable is missing', () => {
    delete process.env.AWS_REGION;

    expect(() => loadWebSocketFunctionEnv()).toThrow(
      'Missing required environment variable: AWS_REGION'
    );
  });

  it('throws when token use is invalid', () => {
    process.env.COGNITO_TOKEN_USE = 'refresh';

    expect(() => loadWebSocketFunctionEnv()).toThrow(
      'COGNITO_TOKEN_USE must be either access or id'
    );
  });
});
