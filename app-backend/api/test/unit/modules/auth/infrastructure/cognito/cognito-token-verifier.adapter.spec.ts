const mockVerify = jest.fn();
const mockCreate = jest.fn(() => ({ verify: mockVerify }));

jest.mock('aws-jwt-verify', () => ({
  CognitoJwtVerifier: {
    create: mockCreate
  }
}));

import type { ConfigService } from '@nestjs/config';
import type { InvalidTokenError } from '@/modules/auth/domain/errors/invalid-token.error';
import { CognitoTokenVerifierAdapter } from '@/modules/auth/infrastructure/cognito/cognito-token-verifier.adapter';

describe('CognitoTokenVerifierAdapter', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  function createAdapter() {
    const configService = {
      getOrThrow: jest.fn((key: string) => {
        const values: Record<string, string> = {
          'auth.cognitoUserPoolId': 'pool',
          'auth.cognitoTokenUse': 'id',
          'auth.cognitoClientId': 'client'
        };

        return values[key];
      })
    } as unknown as ConfigService;

    return {
      adapter: new CognitoTokenVerifierAdapter(configService),
      configService
    };
  }

  it('creates the verifier from auth config', () => {
    const { configService } = createAdapter();

    expect(configService.getOrThrow).toHaveBeenCalledWith(
      'auth.cognitoUserPoolId'
    );
    expect(configService.getOrThrow).toHaveBeenCalledWith('auth.cognitoTokenUse');
    expect(configService.getOrThrow).toHaveBeenCalledWith('auth.cognitoClientId');
    expect(mockCreate).toHaveBeenCalledWith({
      userPoolId: 'pool',
      tokenUse: 'id',
      clientId: 'client'
    });
  });

  it('maps Cognito claims into authenticated user output', async () => {
    mockVerify.mockResolvedValue({
      sub: 42,
      email: 'user@example.test',
      email_verified: true,
      name: 'User Name',
      picture: 'https://example.test/avatar.png',
      identities: [{ providerName: 'Google' }],
      'cognito:groups': ['admin', 123]
    });
    const { adapter } = createAdapter();

    await expect(adapter.verify('token')).resolves.toEqual({
      sub: '42',
      email: 'user@example.test',
      emailVerified: true,
      name: 'User Name',
      pictureUrl: 'https://example.test/avatar.png',
      provider: 'Google',
      groups: ['admin', '123']
    });
  });

  it('omits optional claims when Cognito returns unexpected shapes', async () => {
    mockVerify.mockResolvedValue({
      sub: 'user',
      email: false,
      email_verified: 'maybe',
      name: 123,
      picture: null,
      identities: [{ providerName: false }],
      'cognito:groups': 'admin'
    });
    const { adapter } = createAdapter();

    await expect(adapter.verify('token')).resolves.toEqual({
      sub: 'user',
      email: undefined,
      emailVerified: undefined,
      name: undefined,
      pictureUrl: undefined,
      provider: undefined,
      groups: []
    });
  });

  it('returns no provider when identities are missing', async () => {
    mockVerify.mockResolvedValue({ sub: 'user', identities: [] });
    const { adapter } = createAdapter();

    await expect(adapter.verify('token')).resolves.toEqual(
      expect.objectContaining({
        emailVerified: undefined,
        provider: undefined
      })
    );
  });

  it('maps string email_verified claims safely', async () => {
    mockVerify.mockResolvedValue({
      sub: 'user',
      email_verified: 'false'
    });
    const { adapter } = createAdapter();

    await expect(adapter.verify('token')).resolves.toEqual(
      expect.objectContaining({
        sub: 'user',
        emailVerified: false
      })
    );
  });

  it('wraps verifier failures as InvalidTokenError', async () => {
    const cause = new Error('bad signature');
    mockVerify.mockRejectedValue(cause);
    const { adapter } = createAdapter();

    await expect(adapter.verify('token')).rejects.toMatchObject({
      name: 'InvalidTokenError',
      cause
    } satisfies Partial<InvalidTokenError>);
  });
});
