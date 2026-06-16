import type { IAuthenticatedUserOutput } from '../dto/authenticated-user.output';

/**
 * Verifies trusted identity tokens and returns application-level claims.
 */
export interface ITokenVerifierPort {
  /**
   * Verifies token signature, issuer, audience, and expiration.
   *
   * @param token - Bearer token without the `Bearer` prefix.
   * @returns Verified identity claims used by the API.
   */
  verify(token: string): Promise<IAuthenticatedUserOutput>;
}

/**
 * Dependency injection token for the token verifier port.
 */
export const TOKEN_VERIFIER_PORT = Symbol('TOKEN_VERIFIER_PORT');
