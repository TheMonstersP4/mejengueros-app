import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { ErrorLogLevel } from '@/shared/domain/errors/base.error';
import { InvalidTokenError } from '@/modules/auth/domain/errors/invalid-token.error';
import { MissingBearerTokenError } from '@/modules/auth/domain/errors/missing-bearer-token.error';

describe('auth errors', () => {
  it('creates invalid token errors with safe and internal metadata', () => {
    const cause = new Error('jwt expired');
    const error = new InvalidTokenError(cause);

    expect(error.name).toBe('InvalidTokenError');
    expect(error.code).toBe(APP_ERROR_CODES.AUTH_INVALID_TOKEN);
    expect(error.kind).toBe('auth');
    expect(error.userMessage).toBe('Authentication token is invalid or expired.');
    expect(error.internalMessage).toBe('Cognito token verification failed.');
    expect(error.logLevel).toBe(ErrorLogLevel.Warn);
    expect(error.cause).toBe(cause);
  });

  it('creates missing bearer token errors', () => {
    const error = new MissingBearerTokenError();

    expect(error.name).toBe('MissingBearerTokenError');
    expect(error.code).toBe(APP_ERROR_CODES.AUTH_MISSING_TOKEN);
    expect(error.kind).toBe('auth');
    expect(error.userMessage).toBe('Authentication token is required.');
    expect(error.internalMessage).toBe('Missing Authorization bearer token.');
    expect(error.logLevel).toBe(ErrorLogLevel.Warn);
  });
});
