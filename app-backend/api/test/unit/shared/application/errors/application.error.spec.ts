import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { ApplicationError } from '@/shared/application/errors/application.error';

class TestApplicationError extends ApplicationError {
  constructor() {
    super({
      code: APP_ERROR_CODES.CONFLICT,
      kind: 'conflict',
      userMessage: 'Application conflict.'
    });
  }
}

describe('ApplicationError', () => {
  it('extends the shared base error contract', () => {
    const error = new TestApplicationError();

    expect(error.code).toBe(APP_ERROR_CODES.CONFLICT);
    expect(error.kind).toBe('conflict');
    expect(error.internalMessage).toBe('Application conflict.');
  });
});
