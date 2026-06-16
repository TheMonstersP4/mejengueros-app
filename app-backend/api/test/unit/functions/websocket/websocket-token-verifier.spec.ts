const mockVerify = jest.fn();
const mockCreate = jest.fn(() => ({ verify: mockVerify }));

jest.mock('aws-jwt-verify', () => ({
  CognitoJwtVerifier: {
    create: mockCreate
  }
}));

import { WebSocketTokenVerifier } from '@/functions/websocket/websocket-token-verifier';

describe('WebSocketTokenVerifier', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('creates a Cognito verifier from environment settings', () => {
    new WebSocketTokenVerifier({
      awsRegion: 'us-east-1',
      cognitoUserPoolId: 'pool',
      cognitoClientId: 'client',
      cognitoTokenUse: 'access',
      connectionsTableName: 'connections',
      connectionTtlSeconds: 60
    });

    expect(mockCreate).toHaveBeenCalledWith({
      userPoolId: 'pool',
      tokenUse: 'access',
      clientId: 'client'
    });
  });

  it('maps verified token claims to websocket identity', async () => {
    mockVerify.mockResolvedValue({
      sub: 123,
      email: 'user@example.test',
      name: 'User'
    });
    const verifier = new WebSocketTokenVerifier({
      awsRegion: 'us-east-1',
      cognitoUserPoolId: 'pool',
      cognitoClientId: 'client',
      cognitoTokenUse: 'id',
      connectionsTableName: 'connections',
      connectionTtlSeconds: 60
    });

    await expect(verifier.verify('token')).resolves.toEqual({
      sub: '123',
      email: 'user@example.test',
      name: 'User'
    });
    expect(mockVerify).toHaveBeenCalledWith('token');
  });

  it('omits non-string email claims', async () => {
    mockVerify.mockResolvedValue({ sub: 'user', email: false });
    const verifier = new WebSocketTokenVerifier({
      awsRegion: 'us-east-1',
      cognitoUserPoolId: 'pool',
      cognitoClientId: 'client',
      cognitoTokenUse: 'id',
      connectionsTableName: 'connections',
      connectionTtlSeconds: 60
    });

    await expect(verifier.verify('token')).resolves.toEqual({
      sub: 'user',
      email: undefined,
      name: undefined
    });
  });
});
