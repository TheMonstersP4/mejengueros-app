import 'reflect-metadata';
import type { INestApplicationContext } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { loadDatabaseUrlFromSecret } from './database-secret';
import { ReservationCompletionWorkerModule } from '@/modules/reservations/interfaces/worker/reservation-completion-worker.module';

export async function createReservationCompletionWorkerApplicationContext(): Promise<INestApplicationContext> {
  await loadDatabaseUrlFromSecret();

  const app = await NestFactory.createApplicationContext(ReservationCompletionWorkerModule, {
    bufferLogs: true
  });

  return app;
}
