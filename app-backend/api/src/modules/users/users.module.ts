import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { USER_REPOSITORY } from './domain/repositories/user.repository';
import { PrismaUserRepository } from './infrastructure/persistence/prisma-user.repository';
import { ListUsersUseCase } from './application/use-cases/list-users.use-case';
import { SyncAuthenticatedUserUseCase } from './application/use-cases/sync-authenticated-user.use-case';
import { UsersController } from './interfaces/http/controllers/users.controller';

@Module({
  imports: [AuthModule],
  controllers: [UsersController],
  providers: [
    ListUsersUseCase,
    SyncAuthenticatedUserUseCase,
    {
      provide: USER_REPOSITORY,
      useClass: PrismaUserRepository
    }
  ],
  exports: [SyncAuthenticatedUserUseCase]
})
export class UsersModule {}
