import { Module, forwardRef } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { FilesModule } from '../files/files.module';
import { USER_REPOSITORY } from './domain/repositories/user.repository';
import { PrismaUserRepository } from './infrastructure/persistence/prisma-user.repository';
import { DeactivateUserUseCase } from './application/use-cases/deactivate-user.use-case';
import { ListUsersUseCase } from './application/use-cases/list-users.use-case';
import { SyncAuthenticatedUserUseCase } from './application/use-cases/sync-authenticated-user.use-case';
import { UpdateMyProfileImageUseCase } from './application/use-cases/update-my-profile-image.use-case';
import { UserProfileService } from './application/services/user-profile.service';
import { UsersController } from './interfaces/http/controllers/users.controller';
import { ActiveUserAccountGuard } from './interfaces/http/guards/active-user-account.guard';

@Module({
  imports: [AuthModule, forwardRef(() => FilesModule)],
  controllers: [UsersController],
  providers: [
    ListUsersUseCase,
    DeactivateUserUseCase,
    SyncAuthenticatedUserUseCase,
    UpdateMyProfileImageUseCase,
    UserProfileService,
    ActiveUserAccountGuard,
    {
      provide: USER_REPOSITORY,
      useClass: PrismaUserRepository
    }
  ],
  exports: [SyncAuthenticatedUserUseCase, ActiveUserAccountGuard, USER_REPOSITORY]
})
export class UsersModule {}
