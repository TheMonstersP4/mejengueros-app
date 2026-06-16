import type { INestApplication } from '@nestjs/common';
import { configureShutdown } from '@/bootstrap/shutdown';

describe('configureShutdown', () => {
  it('enables Nest shutdown hooks', () => {
    const app = {
      enableShutdownHooks: jest.fn()
    } as unknown as INestApplication;

    configureShutdown(app);

    expect(app.enableShutdownHooks).toHaveBeenCalledTimes(1);
  });
});
