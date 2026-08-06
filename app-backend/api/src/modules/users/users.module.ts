import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { FilesModule } from '../files/files.module';
import { USER_REPOSITORY } from './domain/repositories/user.repository';
import { PrismaUserRepository } from './infrastructure/persistence/prisma-user.repository';
import { ListUsersUseCase } from './application/use-cases/list-users.use-case';
import { SyncAuthenticatedUserUseCase } from './application/use-cases/sync-authenticated-user.use-case';
import { UpdateMyProfileImageUseCase } from './application/use-cases/update-my-profile-image.use-case';
import { UpdateUserAccountUseCase } from './application/use-cases/update-user-account.use-case';
import { UserProfileService } from './application/services/user-profile.service';
import { UsersController } from './interfaces/http/controllers/users.controller';

@Module({
  imports: [AuthModule, FilesModule],
  controllers: [UsersController],
  providers: [
    ListUsersUseCase,
    SyncAuthenticatedUserUseCase,
    UpdateMyProfileImageUseCase,
    UpdateUserAccountUseCase,
    UserProfileService,
    {
      provide: USER_REPOSITORY,
      useClass: PrismaUserRepository
    }
  ],
  exports: [SyncAuthenticatedUserUseCase]
})
export class UsersModule {}
