import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import {
  DeleteCommand,
  DynamoDBDocumentClient,
  GetCommand,
  PutCommand,
  QueryCommand
} from '@aws-sdk/lib-dynamodb';
import type { IWebSocketIdentity } from './websocket-token-verifier';

export const DEFAULT_WEBSOCKET_ROOM_ID = 'dev';
export const WEBSOCKET_ROOM_INDEX_NAME = 'byRoomId';

/**
 * Input required to persist an active WebSocket connection.
 */
export interface IStoreConnectionInput {
  /**
   * API Gateway connection ID.
   */
  connectionId: string;

  /**
   * Authenticated identity associated with the connection.
   */
  identity: IWebSocketIdentity;

  /**
   * Logical room where the connection participates.
   */
  roomId?: string;

  /**
   * Time-to-live in seconds for stale connection cleanup.
   */
  ttlSeconds: number;
}

/**
 * Active WebSocket connection persisted in DynamoDB.
 */
export interface IStoredConnection {
  /**
   * API Gateway connection ID.
   */
  connectionId: string;

  /**
   * Stable Cognito subject.
   */
  userId: string;

  /**
   * Logical room where the connection participates.
   */
  roomId: string;

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
 * DynamoDB store for active API Gateway WebSocket connections.
 */
export class WebSocketConnectionStore {
  private readonly documentClient: DynamoDBDocumentClient;

  constructor(
    awsRegion: string,
    private readonly tableName: string
  ) {
    this.documentClient = DynamoDBDocumentClient.from(
      new DynamoDBClient({ region: awsRegion })
    );
  }

  /**
   * Stores a connection with a TTL for automatic cleanup.
   *
   * @param input - Connection data to persist.
   */
  async store(input: IStoreConnectionInput): Promise<void> {
    const connectedAt = new Date().toISOString();
    const expiresAt = Math.floor(Date.now() / 1000) + input.ttlSeconds;
    const roomId = input.roomId ?? DEFAULT_WEBSOCKET_ROOM_ID;

    await this.documentClient.send(
      new PutCommand({
        TableName: this.tableName,
        Item: {
          connectionId: input.connectionId,
          roomId,
          userId: input.identity.sub,
          email: input.identity.email,
          name: input.identity.name,
          connectedAt,
          expiresAt
        }
      })
    );
  }

  /**
   * Removes a connection when API Gateway sends `$disconnect`.
   *
   * @param connectionId - API Gateway connection ID.
   */
  async remove(connectionId: string): Promise<void> {
    await this.documentClient.send(
      new DeleteCommand({
        TableName: this.tableName,
        Key: { connectionId }
      })
    );
  }

  /**
   * Finds a connection by API Gateway connection ID.
   *
   * @param connectionId - API Gateway connection ID.
   * @returns Stored connection data when the record exists.
   */
  async findById(connectionId: string): Promise<IStoredConnection | undefined> {
    const response = await this.documentClient.send(
      new GetCommand({
        TableName: this.tableName,
        Key: { connectionId }
      })
    );

    const item = response.Item;

    if (!item || typeof item.connectionId !== 'string' || typeof item.userId !== 'string') {
      return undefined;
    }

    return {
      connectionId: String(item.connectionId),
      userId: String(item.userId),
      roomId: typeof item.roomId === 'string' ? item.roomId : DEFAULT_WEBSOCKET_ROOM_ID,
      email: typeof item.email === 'string' ? item.email : undefined,
      name: typeof item.name === 'string' ? item.name : undefined
    };
  }

  /**
   * Returns currently known connections.
   *
   * @param roomId - Room identifier used by the DynamoDB room index.
   * @returns Active connection records in the room that have not expired by TTL timestamp.
   */
  async listActive(roomId = DEFAULT_WEBSOCKET_ROOM_ID): Promise<IStoredConnection[]> {
    const now = Math.floor(Date.now() / 1000);
    const response = await this.documentClient.send(
      new QueryCommand({
        TableName: this.tableName,
        IndexName: WEBSOCKET_ROOM_INDEX_NAME,
        KeyConditionExpression: 'roomId = :roomId',
        FilterExpression: 'attribute_not_exists(expiresAt) OR expiresAt > :now',
        ExpressionAttributeValues: {
          ':roomId': roomId,
          ':now': now
        }
      })
    );

    return (response.Items ?? [])
      .filter((item): item is Record<string, unknown> => {
        return typeof item.connectionId === 'string' && typeof item.userId === 'string';
      })
      .map((item) => ({
        connectionId: String(item.connectionId),
        userId: String(item.userId),
        roomId: typeof item.roomId === 'string' ? item.roomId : roomId,
        email: typeof item.email === 'string' ? item.email : undefined,
        name: typeof item.name === 'string' ? item.name : undefined
      }));
  }
}
