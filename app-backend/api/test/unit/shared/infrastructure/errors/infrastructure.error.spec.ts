import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { InfrastructureError } from '@/shared/infrastructure/errors/infrastructure.error';

class TestInfrastructureError extends InfrastructureError {
  constructor() {
    super({
      code: APP_ERROR_CODES.EXTERNAL_SERVICE_ERROR,
      kind: 'external',
      userMessage: 'Provider failed.'
    });
  }
}

describe('InfrastructureError', () => {
  it('extends the shared base error contract', () => {
    const error = new TestInfrastructureError();

    expect(error.code).toBe(APP_ERROR_CODES.EXTERNAL_SERVICE_ERROR);
    expect(error.kind).toBe('external');
    expect(error.internalMessage).toBe('Provider failed.');
  });
});
