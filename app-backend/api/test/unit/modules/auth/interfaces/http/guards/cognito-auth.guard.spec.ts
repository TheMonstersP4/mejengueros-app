import type { ExecutionContext } from '@nestjs/common';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { MissingBearerTokenError } from '@/modules/auth/domain/errors/missing-bearer-token.error';
import { CognitoAuthGuard } from '@/modules/auth/interfaces/http/guards/cognito-auth.guard';

describe('CognitoAuthGuard', () => {
  const verifiedUser = {
    sub: 'user-1',
    email: 'user@example.test',
    groups: ['admin']
  };

  function createContext(request: unknown): ExecutionContext {
    return {
      switchToHttp: () => ({
        getRequest: () => request
      })
    } as unknown as ExecutionContext;
  }

  it('verifies bearer tokens and attaches the current user', async () => {
    const request = {
      headers: {
        authorization: 'Bearer token-value   '
      }
    };
    const tokenVerifier = {
      verify: jest.fn().mockResolvedValue(verifiedUser)
    } satisfies ITokenVerifierPort;
    const guard = new CognitoAuthGuard(tokenVerifier);

    await expect(guard.canActivate(createContext(request))).resolves.toBe(true);

    expect(tokenVerifier.verify).toHaveBeenCalledWith('token-value');
    expect(request).toHaveProperty('user', verifiedUser);
  });

  it.each([
    {},
    { headers: {} },
    { headers: { authorization: 'Basic token' } },
    { headers: { authorization: 'Bearer ' } }
  ])('rejects requests without a usable bearer token %#', async (request) => {
    const tokenVerifier = {
      verify: jest.fn()
    } satisfies ITokenVerifierPort;
    const guard = new CognitoAuthGuard(tokenVerifier);

    await expect(
      guard.canActivate(createContext({ headers: {}, ...request }))
    ).rejects.toBeInstanceOf(MissingBearerTokenError);
    expect(tokenVerifier.verify).not.toHaveBeenCalled();
  });
});
