import { Inject, Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { CognitoJwtVerifier } from 'aws-jwt-verify';
import type { IAuthenticatedUserOutput } from '../../application/dto/authenticated-user.output';
import type { ITokenVerifierPort } from '../../application/ports/token-verifier.port';
import { InvalidTokenError } from '../../domain/errors/invalid-token.error';

/**
 * Cognito implementation of the token verifier port.
 *
 * @remarks
 * This adapter validates Cognito-issued JWTs only. Google and Microsoft tokens
 * are handled by Cognito as identity providers before reaching the API.
 */
@Injectable()
export class CognitoTokenVerifierAdapter implements ITokenVerifierPort {
  private readonly verifier;

  constructor(
    @Inject(ConfigService)
    private readonly configService: ConfigService
  ) {
    this.verifier = CognitoJwtVerifier.create({
      userPoolId: this.configService.getOrThrow<string>('auth.cognitoUserPoolId'),
      tokenUse: this.configService.getOrThrow<'access' | 'id'>('auth.cognitoTokenUse'),
      clientId: this.configService.getOrThrow<string>('auth.cognitoClientId')
    });
  }

  /**
   * Verifies a Cognito JWT and maps claims to application identity data.
   *
   * @param token - Bearer token without the `Bearer` prefix.
   * @returns Normalized authenticated user claims.
   * @throws InvalidTokenError when signature, issuer, audience, or expiration validation fails.
   */
  async verify(token: string): Promise<IAuthenticatedUserOutput> {
    try {
      const payload = await this.verifier.verify(token);
      const groupsClaim = payload['cognito:groups'];
      const groups = Array.isArray(groupsClaim) ? groupsClaim.map(String) : [];

      return {
        sub: String(payload.sub),
        email: typeof payload.email === 'string' ? payload.email : undefined,
        name: typeof payload.name === 'string' ? payload.name : undefined,
        pictureUrl: typeof payload.picture === 'string' ? payload.picture : undefined,
        provider: this.resolveProvider(payload.identities),
        groups
      };
    } catch (error) {
      throw new InvalidTokenError(error);
    }
  }

  private resolveProvider(identities: unknown): string | undefined {
    if (!Array.isArray(identities) || identities.length === 0) {
      return undefined;
    }

    const firstIdentity = identities[0] as { providerName?: unknown };
    return typeof firstIdentity.providerName === 'string'
      ? firstIdentity.providerName
      : undefined;
  }
}
