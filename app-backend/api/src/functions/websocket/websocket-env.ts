/**
 * Environment required by WebSocket Lambda handlers.
 */
export interface IWebSocketFunctionEnv {
  /**
   * AWS region where DynamoDB and Cognito are configured.
   */
  awsRegion: string;

  /**
   * Cognito user pool ID used to verify tokens.
   */
  cognitoUserPoolId: string;

  /**
   * Cognito app client ID expected in tokens.
   */
  cognitoClientId: string;

  /**
   * Cognito token use accepted by the handler.
   */
  cognitoTokenUse: 'access' | 'id';

  /**
   * DynamoDB table used to store active WebSocket connections.
   */
  connectionsTableName: string;

  /**
   * Time-to-live in seconds for connection records.
   */
  connectionTtlSeconds: number;
}

/**
 * Reads and validates WebSocket Lambda environment variables.
 *
 * @returns Runtime configuration for WebSocket handlers.
 * @throws Error when a required environment variable is missing or invalid.
 */
export function loadWebSocketFunctionEnv(): IWebSocketFunctionEnv {
  return {
    awsRegion: readRequiredEnv('AWS_REGION'),
    cognitoUserPoolId: readRequiredEnv('COGNITO_USER_POOL_ID'),
    cognitoClientId: readRequiredEnv('COGNITO_CLIENT_ID'),
    cognitoTokenUse: readTokenUse(),
    connectionsTableName: readRequiredEnv('WEBSOCKET_CONNECTIONS_TABLE_NAME'),
    connectionTtlSeconds: Number(process.env.WEBSOCKET_CONNECTION_TTL_SECONDS ?? 86400)
  };
}

function readRequiredEnv(name: string): string {
  const value = process.env[name];

  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }

  return value;
}

function readTokenUse(): 'access' | 'id' {
  const tokenUse = process.env.COGNITO_TOKEN_USE ?? 'id';

  if (tokenUse !== 'access' && tokenUse !== 'id') {
    throw new Error('COGNITO_TOKEN_USE must be either access or id');
  }

  return tokenUse;
}
