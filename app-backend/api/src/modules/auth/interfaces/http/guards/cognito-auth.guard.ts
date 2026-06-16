import type { CanActivate, ExecutionContext } from '@nestjs/common';
import { Inject, Injectable } from '@nestjs/common';
import type { FastifyRequest } from 'fastify';
import type { ITokenVerifierPort } from '../../../application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '../../../application/ports/token-verifier.port';
import type { IAuthenticatedUserOutput } from '../../../application/dto/authenticated-user.output';
import { MissingBearerTokenError } from '../../../domain/errors/missing-bearer-token.error';

/**
 * Protects HTTP routes with Cognito JWT authentication.
 *
 * @remarks
 * The guard verifies the bearer token through the auth application port and
 * attaches normalized identity claims to the request for controllers.
 */
@Injectable()
export class CognitoAuthGuard implements CanActivate {
  constructor(
    @Inject(TOKEN_VERIFIER_PORT)
    private readonly tokenVerifier: ITokenVerifierPort
  ) {}

  /**
   * Verifies the bearer token and attaches the current user to the request.
   *
   * @param context - NestJS execution context for the current HTTP request.
   * @returns `true` when the request is authenticated.
   * @throws MissingBearerTokenError when the Authorization header is missing.
   * @throws InvalidTokenError when Cognito rejects the token.
   */
  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<
      FastifyRequest & { user?: IAuthenticatedUserOutput }
    >();
    const token = this.extractBearerToken(request);

    if (!token) {
      throw new MissingBearerTokenError();
    }

    request.user = await this.tokenVerifier.verify(token);
    return true;
  }

  private extractBearerToken(request: FastifyRequest): string | null {
    const authorization = request.headers.authorization;

    if (!authorization?.startsWith('Bearer ')) {
      return null;
    }

    return authorization.slice('Bearer '.length).trim();
  }
}
