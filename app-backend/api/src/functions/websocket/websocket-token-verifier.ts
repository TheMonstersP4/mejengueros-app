import { CognitoJwtVerifier } from 'aws-jwt-verify';
import type { IWebSocketFunctionEnv } from './websocket-env';

/**
 * Identity claims accepted by WebSocket connection handlers.
 */
export interface IWebSocketIdentity {
  /**
   * Stable Cognito subject.
   */
  sub: string;

  /**
   * Verified email claim when available.
   */
  email?: string;

  /**
   * Display name claim when available.
   */
  name?: string;
}

/**
 * Verifies Cognito JWTs for API Gateway WebSocket connections.
 */
export class WebSocketTokenVerifier {
  private readonly verifier;

  constructor(env: IWebSocketFunctionEnv) {
    this.verifier = CognitoJwtVerifier.create({
      userPoolId: env.cognitoUserPoolId,
      tokenUse: env.cognitoTokenUse,
      clientId: env.cognitoClientId
    });
  }

  /**
   * Verifies a token and returns the identity used by the connection table.
   *
   * @param token - Cognito token received from query string or Authorization header.
   * @returns WebSocket identity claims.
   */
  async verify(token: string): Promise<IWebSocketIdentity> {
    const payload = await this.verifier.verify(token);

    return {
      sub: String(payload.sub),
      email: typeof payload.email === 'string' ? payload.email : undefined,
      name: typeof payload.name === 'string' ? payload.name : undefined
    };
  }
}
