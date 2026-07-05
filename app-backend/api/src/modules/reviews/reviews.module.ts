import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { ListOwnerCourtReviewsUseCase } from './application/use-cases/list-owner-court-reviews.use-case';
import { REVIEW_REPOSITORY } from './domain/repositories/review.repository';
import { PrismaReviewRepository } from './infrastructure/persistence/prisma-review.repository';
import { OwnerReviewsController } from './interfaces/http/controllers/owner-reviews.controller';

/**
 * Feature module exposing the owner reviews dashboard endpoint.
 */
@Module({
  imports: [AuthModule],
  controllers: [OwnerReviewsController],
  providers: [
    ListOwnerCourtReviewsUseCase,
    {
      provide: REVIEW_REPOSITORY,
      useClass: PrismaReviewRepository
    }
  ]
})
export class ReviewsModule {}
