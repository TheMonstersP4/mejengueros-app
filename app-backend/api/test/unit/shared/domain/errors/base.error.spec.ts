import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { BaseError } from '@/shared/domain/errors/base.error';

class TestError extends BaseError {
  constructor() {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: 'The request is invalid.',
      internalMessage: 'Domain validation failed.',
      logContext: { field: 'email' }
    });
  }
}

describe('BaseError', () => {
  it('keeps public and internal error metadata separated', () => {
    const error = new TestError();

    expect(error.code).toBe(APP_ERROR_CODES.VALIDATION_FAILED);
    expect(error.kind).toBe('validation');
    expect(error.userMessage).toBe('The request is invalid.');
    expect(error.internalMessage).toBe('Domain validation failed.');
    expect(error.logContext).toEqual({ field: 'email' });
    expect(error.isOperational).toBe(true);
  });
});
