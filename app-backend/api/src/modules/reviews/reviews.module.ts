import { Module } from '@nestjs/common';
import { PrismaModule } from '@/shared/infrastructure/database/prisma.module';
import { AuthModule } from '../auth/auth.module';
import { FilesModule } from '../files/files.module';
import { UsersModule } from '../users/users.module';
import { CreateReviewUseCase } from './application/use-cases/create-review.use-case';
import { GetLatestReviewableReservationUseCase } from './application/use-cases/get-latest-reviewable-reservation.use-case';
import { ListOwnerCourtReviewsUseCase } from './application/use-cases/list-owner-court-reviews.use-case';
import { ListPublicCourtReviewsUseCase } from './application/use-cases/list-public-court-reviews.use-case';
import { REVIEW_REPOSITORY } from './domain/repositories/review.repository';
import { PrismaReviewRepository } from './infrastructure/persistence/prisma-review.repository';
import { OwnerReviewsController } from './interfaces/http/controllers/owner-reviews.controller';
import { PublicCourtReviewsController } from './interfaces/http/controllers/public-court-reviews.controller';
import { ReviewsController } from './interfaces/http/controllers/reviews.controller';

/**
 * Feature module exposing the owner reviews dashboard endpoint
 * together with the player review submission endpoints.
 */
@Module({
  imports: [AuthModule, UsersModule, FilesModule, PrismaModule],
  controllers: [OwnerReviewsController, PublicCourtReviewsController, ReviewsController],
  providers: [
    CreateReviewUseCase,
    GetLatestReviewableReservationUseCase,
    ListOwnerCourtReviewsUseCase,
    ListPublicCourtReviewsUseCase,
    {
      provide: REVIEW_REPOSITORY,
      useClass: PrismaReviewRepository
    }
  ]
})
export class ReviewsModule {}
