import { Module } from '@nestjs/common';
import { AuthController } from './interfaces/http/controllers/auth.controller';
import { CognitoAuthGuard } from './interfaces/http/guards/cognito-auth.guard';
import { CognitoTokenVerifierAdapter } from './infrastructure/cognito/cognito-token-verifier.adapter';
import { TOKEN_VERIFIER_PORT } from './application/ports/token-verifier.port';

@Module({
  controllers: [AuthController],
  providers: [
    CognitoAuthGuard,
    {
      provide: TOKEN_VERIFIER_PORT,
      useClass: CognitoTokenVerifierAdapter
    }
  ],
  exports: [TOKEN_VERIFIER_PORT, CognitoAuthGuard]
})
export class AuthModule {}
