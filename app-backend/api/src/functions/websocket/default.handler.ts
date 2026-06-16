import type { APIGatewayProxyWebsocketHandlerV2 } from 'aws-lambda';
import {
  WebSocketBroadcaster,
  buildManagementEndpoint,
  mapRoomUsers
} from './websocket-broadcaster';
import {
  DEFAULT_WEBSOCKET_ROOM_ID,
  WebSocketConnectionStore
} from './connection-store';
import { jsonResponse } from './http-response';
import { loadWebSocketFunctionEnv } from './websocket-env';

const env = loadWebSocketFunctionEnv();
const connectionStore = new WebSocketConnectionStore(
  env.awsRegion,
  env.connectionsTableName
);

interface IWebSocketClientMessage {
  action?: string;
  message?: string;
  roomId?: string;
  sentAt?: string;
}

/**
 * Handles room messages for the WebSocket chat.
 *
 * @remarks
 * API Gateway sends unmatched actions to `$default`. The handler supports
 * room join and chat messages without bootstrapping the Nest HTTP app.
 */
export const handler: APIGatewayProxyWebsocketHandlerV2 = async (event) => {
  const connectionId = event.requestContext.connectionId;
  const domainName = event.requestContext.domainName;
  const stage = event.requestContext.stage;

  if (!connectionId || !domainName || !stage) {
    return jsonResponse(400, { message: 'Missing WebSocket context' });
  }

  const clientMessage = parseClientMessage(event.body);
  const roomId = clientMessage.roomId ?? DEFAULT_WEBSOCKET_ROOM_ID;
  const connections = await connectionStore.listActive(roomId);
  const broadcaster = new WebSocketBroadcaster(
    buildManagementEndpoint(domainName, stage)
  );

  await removeStaleConnections(
    await broadcaster.broadcast(connections, buildPayload(clientMessage, connectionId, roomId, connections))
  );

  return jsonResponse(200, { message: 'Delivered' });
};

function buildPayload(
  clientMessage: IWebSocketClientMessage,
  connectionId: string,
  roomId: string,
  connections: Awaited<ReturnType<WebSocketConnectionStore['listActive']>>
): Record<string, unknown> {
  if (clientMessage.action === 'sendMessage' && clientMessage.message) {
    const sender = connections.find((connection) => connection.connectionId === connectionId);

    return {
      type: 'message',
      roomId,
      message: clientMessage.message,
      sentAt: clientMessage.sentAt ?? new Date().toISOString(),
      sender: sender
        ? {
            sub: sender.userId,
            email: sender.email,
            name: sender.name
          }
        : undefined
    };
  }

  return {
    type: 'presence',
    roomId,
    users: mapRoomUsers(connections)
  };
}

function parseClientMessage(body: string | undefined): IWebSocketClientMessage {
  if (!body) {
    return {};
  }

  try {
    return JSON.parse(body) as IWebSocketClientMessage;
  } catch {
    return {};
  }
}

async function removeStaleConnections(connectionIds: string[]): Promise<void> {
  await Promise.all(
    connectionIds.map((connectionId) => connectionStore.remove(connectionId))
  );
}
