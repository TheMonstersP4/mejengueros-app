import type { APIGatewayProxyWebsocketHandlerV2 } from 'aws-lambda';
import {
  DEFAULT_WEBSOCKET_ROOM_ID,
  WebSocketConnectionStore
} from './connection-store';
import { jsonResponse } from './http-response';
import { loadWebSocketFunctionEnv } from './websocket-env';
import {
  WebSocketBroadcaster,
  buildManagementEndpoint,
  mapRoomUsers
} from './websocket-broadcaster';

const env = loadWebSocketFunctionEnv();
const connectionStore = new WebSocketConnectionStore(
  env.awsRegion,
  env.connectionsTableName
);

/**
 * Handles API Gateway WebSocket `$disconnect` events.
 *
 * @remarks
 * Disconnect events are best-effort in API Gateway. The connection table also
 * uses TTL to clean up stale records that are not removed by this handler.
 */
export const handler: APIGatewayProxyWebsocketHandlerV2 = async (event) => {
  const connectionId = event.requestContext.connectionId;
  const domainName = event.requestContext.domainName;
  const stage = event.requestContext.stage;

  if (!connectionId) {
    return jsonResponse(400, { message: 'Missing connection id' });
  }

  const connection = await connectionStore.findById(connectionId);

  await connectionStore.remove(connectionId);
  await broadcastPresence(domainName, stage, connection?.roomId);

  return jsonResponse(200, { message: 'Disconnected' });
};

async function broadcastPresence(
  domainName: string | undefined,
  stage: string | undefined,
  roomId = DEFAULT_WEBSOCKET_ROOM_ID
): Promise<void> {
  if (!domainName || !stage) {
    return;
  }

  const connections = await connectionStore.listActive(roomId);
  const broadcaster = new WebSocketBroadcaster(
    buildManagementEndpoint(domainName, stage)
  );
  const staleConnectionIds = await broadcaster.broadcast(connections, {
    type: 'presence',
    roomId,
    users: mapRoomUsers(connections)
  });

  await Promise.all(
    staleConnectionIds.map((connectionId) => connectionStore.remove(connectionId))
  );
}
