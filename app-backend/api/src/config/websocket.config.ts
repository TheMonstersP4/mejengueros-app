/**
 * WebSocket infrastructure settings.
 */
export interface IWebSocketConfig {
  /**
   * DynamoDB table name for active WebSocket connections.
   */
  connectionsTableName: string;

  /**
   * Time-to-live in seconds for stale connection records.
   */
  connectionTtlSeconds: number;
}

/**
 * Loads WebSocket infrastructure settings.
 *
 * @returns WebSocket config section.
 */
export function websocketConfig(): IWebSocketConfig {
  return {
    connectionsTableName: process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME ?? '',
    connectionTtlSeconds: Number(process.env.WEBSOCKET_CONNECTION_TTL_SECONDS ?? 86400)
  };
}
