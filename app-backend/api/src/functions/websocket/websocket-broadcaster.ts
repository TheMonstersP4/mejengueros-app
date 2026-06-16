import {
  ApiGatewayManagementApiClient,
  DeleteConnectionCommand,
  PostToConnectionCommand
} from '@aws-sdk/client-apigatewaymanagementapi';
import type { IStoredConnection } from './connection-store';

/**
 * User shown in room presence events.
 */
export interface IWebSocketRoomUser {
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
 * Sends messages to connected API Gateway WebSocket clients.
 */
export class WebSocketBroadcaster {
  private readonly client: ApiGatewayManagementApiClient;

  constructor(endpoint: string) {
    this.client = new ApiGatewayManagementApiClient({ endpoint });
  }

  /**
   * Sends a JSON payload to every provided connection.
   *
   * @param connections - Active WebSocket connections.
   * @param payload - JSON-serializable payload.
   * @returns Connection IDs that are no longer valid.
   */
  async broadcast(
    connections: IStoredConnection[],
    payload: Record<string, unknown>
  ): Promise<string[]> {
    const data = Buffer.from(JSON.stringify(payload));
    const staleConnectionIds: string[] = [];

    await Promise.all(
      connections.map(async (connection) => {
        try {
          await this.client.send(
            new PostToConnectionCommand({
              ConnectionId: connection.connectionId,
              Data: data
            })
          );
        } catch (error) {
          if (isGoneException(error)) {
            staleConnectionIds.push(connection.connectionId);
            return;
          }

          throw error;
        }
      })
    );

    return staleConnectionIds;
  }

  /**
   * Disconnects a WebSocket client from API Gateway.
   *
   * @param connectionId - API Gateway connection ID.
   */
  async disconnect(connectionId: string): Promise<void> {
    await this.client.send(
      new DeleteConnectionCommand({
        ConnectionId: connectionId
      })
    );
  }
}

/**
 * Builds the API Gateway Management API endpoint from a WebSocket event.
 *
 * @param domainName - API Gateway domain name.
 * @param stage - API Gateway stage name.
 * @returns Management API HTTPS endpoint.
 */
export function buildManagementEndpoint(domainName: string, stage: string): string {
  return `https://${domainName}/${stage}`;
}

/**
 * Maps connection records into unique room users.
 *
 * @param connections - Active WebSocket connections.
 * @returns Unique users connected to the room.
 */
export function mapRoomUsers(connections: IStoredConnection[]): IWebSocketRoomUser[] {
  const users = new Map<string, IWebSocketRoomUser>();

  for (const connection of connections) {
    users.set(connection.userId, {
      sub: connection.userId,
      email: connection.email,
      name: connection.name
    });
  }

  return [...users.values()].sort((left, right) => {
    const leftName = left.name ?? left.email ?? left.sub;
    const rightName = right.name ?? right.email ?? right.sub;

    return leftName.localeCompare(rightName);
  });
}

function isGoneException(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'name' in error &&
    error.name === 'GoneException'
  );
}
