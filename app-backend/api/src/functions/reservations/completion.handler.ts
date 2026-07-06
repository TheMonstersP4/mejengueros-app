import type { Context, ScheduledEvent } from 'aws-lambda';
import pino from 'pino';
import { createReservationCompletionWorkerApplicationContext } from '@/bootstrap/application-context';
import { CompleteExpiredReservationsUseCase } from '@/modules/reservations/application/use-cases/complete-expired-reservations.use-case';

export interface IReservationCompletionHandlerOutput {
  completedReservationsCount: number;
}

let applicationContextPromise:
  | ReturnType<typeof createReservationCompletionWorkerApplicationContext>
  | undefined;
const logger = pino({
  level: process.env.LOG_LEVEL ?? 'info'
});

export async function handler(
  _event: ScheduledEvent,
  _context: Context
): Promise<IReservationCompletionHandlerOutput> {
  void _event;
  void _context;

  const app = await getApplicationContext();
  const useCase = app.get(CompleteExpiredReservationsUseCase, { strict: false });
  const completedReservationsCount = await useCase.execute();

  logger.info(
    { completedReservationsCount },
    'Expired reservation completion worker finished.'
  );

  return { completedReservationsCount };
}

async function getApplicationContext(): ReturnType<typeof createReservationCompletionWorkerApplicationContext> {
  if (applicationContextPromise == null) {
    applicationContextPromise = createReservationCompletionWorkerApplicationContext().catch(
      (error: unknown) => {
        applicationContextPromise = undefined;
        throw error;
      }
    );
  }

  return applicationContextPromise;
}
