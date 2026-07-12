import { Inject, Injectable } from '@nestjs/common';
import { CLOCK, type IClock } from '@/shared/application/clock/clock.port';
import { CreateReviewPromptNotificationsUseCase } from '@/modules/notifications/application/use-cases/create-review-prompt-notifications.use-case';
import {
  RESERVATION_REPOSITORY,
  type IReservationRepository
} from '../../domain/repositories/reservation.repository';

export interface ICompleteExpiredReservationsOutput {
  completedReservationsCount: number;
  reviewPromptNotificationsCreatedCount: number;
}

@Injectable()
export class CompleteExpiredReservationsUseCase {
  constructor(
    @Inject(RESERVATION_REPOSITORY)
    private readonly reservationRepository: IReservationRepository,
    @Inject(CLOCK)
    private readonly clock: IClock,
    @Inject(CreateReviewPromptNotificationsUseCase)
    private readonly createReviewPromptNotifications: CreateReviewPromptNotificationsUseCase
  ) {}

  async execute(): Promise<ICompleteExpiredReservationsOutput> {
    const completedReservations =
      await this.reservationRepository.completeExpiredReservations({
        now: this.clock.now()
      });
    const reviewPromptNotificationsCreatedCount =
      await this.createReviewPromptNotifications.execute(completedReservations);

    return {
      completedReservationsCount: completedReservations.length,
      reviewPromptNotificationsCreatedCount
    };
  }
}
