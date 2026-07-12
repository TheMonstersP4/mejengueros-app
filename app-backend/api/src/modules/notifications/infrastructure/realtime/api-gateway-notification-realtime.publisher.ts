import {
  ApiGatewayManagementApiClient,
  PostToConnectionCommand
} from '@aws-sdk/client-apigatewaymanagementapi';
import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import {
  DeleteCommand,
  DynamoDBDocumentClient,
  QueryCommand
} from '@aws-sdk/lib-dynamodb';
import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';
import type { INotificationOutput } from '../../application/dto/notification.output';
import type { INotificationRealtimePublisher } from '../../application/ports/notification-realtime-publisher.port';

interface INotificationRealtimePersistenceClient {
  userIdentity: PrismaService['userIdentity'];
}

/**
 * Sends persisted notifications to active API Gateway WebSocket connections.
 */
@Injectable()
export class ApiGatewayNotificationRealtimePublisher
  implements INotificationRealtimePublisher
{
  constructor(
    @Inject(PrismaService)
    private readonly prisma: INotificationRealtimePersistenceClient
  ) {}

  private readonly tableName = process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME ?? '';
  private readonly endpoint = normalizeManagementEndpoint(
    process.env.WEBSOCKET_ENDPOINT ?? ''
  );
  private readonly userIndexName =
    process.env.WEBSOCKET_CONNECTIONS_USER_ID_INDEX_NAME ?? 'byUserId';
  private readonly documentClient = DynamoDBDocumentClient.from(
    new DynamoDBClient({ region: process.env.AWS_REGION ?? process.env.APP_S3_REGION })
  );

  async publish(userId: string, notification: INotificationOutput): Promise<void> {
    if (!this.tableName || !this.endpoint) {
      return;
    }

    const userConnectionKeys = await this.resolveUserConnectionKeys(userId);
    const connections = deduplicateConnections(
      (
        await Promise.all(
          userConnectionKeys.map((connectionUserId) =>
            this.loadUserConnections(connectionUserId)
          )
        )
      ).flat()
    );

    if (connections.length === 0) {
      return;
    }

    const client = new ApiGatewayManagementApiClient({ endpoint: this.endpoint });
    const payload = Buffer.from(
      JSON.stringify({
        type: 'notification.created',
        data: notification
      })
    );
    const staleConnectionIds: string[] = [];

    await Promise.all(
      connections.map(async (connection) => {
        try {
          await client.send(
            new PostToConnectionCommand({
              ConnectionId: connection.connectionId,
              Data: payload
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

    await Promise.all(
      staleConnectionIds.map((connectionId) => this.removeConnection(connectionId))
    );
  }

  private async resolveUserConnectionKeys(userId: string): Promise<string[]> {
    const identities = await this.prisma.userIdentity.findMany({
      where: { userId },
      select: { providerSubject: true }
    });
    const connectionKeys = new Set<string>([userId]);

    for (const identity of identities) {
      connectionKeys.add(identity.providerSubject);
    }

    return [...connectionKeys];
  }

  private async loadUserConnections(
    userId: string
  ): Promise<Array<{ connectionId: string }>> {
    const now = Math.floor(Date.now() / 1000);
    const response = await this.documentClient.send(
      new QueryCommand({
        TableName: this.tableName,
        IndexName: this.userIndexName,
        KeyConditionExpression: 'userId = :userId',
        FilterExpression: 'attribute_not_exists(expiresAt) OR expiresAt > :now',
        ExpressionAttributeValues: {
          ':userId': userId,
          ':now': now
        }
      })
    );

    return (response.Items ?? [])
      .filter((item): item is Record<string, unknown> => {
        return typeof item.connectionId === 'string';
      })
      .map((item) => ({ connectionId: String(item.connectionId) }));
  }

  private async removeConnection(connectionId: string): Promise<void> {
    await this.documentClient.send(
      new DeleteCommand({
        TableName: this.tableName,
        Key: { connectionId }
      })
    );
  }
}

function normalizeManagementEndpoint(websocketEndpoint: string): string {
  if (!websocketEndpoint) {
    return '';
  }

  return websocketEndpoint.replace(/^wss:\/\//, 'https://');
}

function isGoneException(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'name' in error &&
    error.name === 'GoneException'
  );
}

function deduplicateConnections(
  connections: Array<{ connectionId: string }>
): Array<{ connectionId: string }> {
  const seenConnectionIds = new Set<string>();

  return connections.filter((connection) => {
    if (seenConnectionIds.has(connection.connectionId)) {
      return false;
    }

    seenConnectionIds.add(connection.connectionId);
    return true;
  });
}
