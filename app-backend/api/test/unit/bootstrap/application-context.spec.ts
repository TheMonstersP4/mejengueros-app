jest.mock('@nestjs/core', () => ({
  NestFactory: {
    createApplicationContext: jest.fn()
  }
}));

jest.mock('@/bootstrap/database-secret', () => ({
  loadDatabaseUrlFromSecret: jest.fn()
}));

import { NestFactory } from '@nestjs/core';
import { loadDatabaseUrlFromSecret } from '@/bootstrap/database-secret';
import { ReservationCompletionWorkerModule } from '@/modules/reservations/interfaces/worker/reservation-completion-worker.module';
import { createReservationCompletionWorkerApplicationContext } from '@/bootstrap/application-context';

describe('createReservationCompletionWorkerApplicationContext', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('loads the database secret before creating the worker Nest application context', async () => {
    const app = { close: jest.fn() };
    const createApplicationContext = jest.mocked(NestFactory.createApplicationContext);

    createApplicationContext.mockResolvedValue(app as never);

    await expect(createReservationCompletionWorkerApplicationContext()).resolves.toBe(app);

    expect(loadDatabaseUrlFromSecret).toHaveBeenCalledTimes(1);
    expect(createApplicationContext).toHaveBeenCalledWith(
      ReservationCompletionWorkerModule,
      expect.objectContaining({ bufferLogs: true })
    );
  });
});
