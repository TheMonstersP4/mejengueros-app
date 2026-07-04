import { Module } from '@nestjs/common';
import { PrismaModule } from '@/shared/infrastructure/database/prisma.module';
import { AuthModule } from '../auth/auth.module';
import { FilesModule } from '../files/files.module';
import { UsersModule } from '../users/users.module';
import { CreateReviewUseCase } from './application/use-cases/create-review.use-case';
import { GetLatestReviewableReservationUseCase } from './application/use-cases/get-latest-reviewable-reservation.use-case';
import { REVIEW_REPOSITORY } from './domain/repositories/review.repository';
import { PrismaReviewRepository } from './infrastructure/persistence/prisma-review.repository';
import { ReviewsController } from './interfaces/http/controllers/reviews.controller';

@Module({
  imports: [AuthModule, UsersModule, FilesModule, PrismaModule],
  controllers: [ReviewsController],
  providers: [
    CreateReviewUseCase,
    GetLatestReviewableReservationUseCase,
    {
      provide: REVIEW_REPOSITORY,
      useClass: PrismaReviewRepository
    }
  ]
})
export class ReviewsModule {}
