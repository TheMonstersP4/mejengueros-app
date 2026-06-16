import type { APIGatewayProxyWebsocketHandlerV2 } from 'aws-lambda';
import { jsonResponse } from './http-response';
import {
  DEFAULT_WEBSOCKET_ROOM_ID,
  WebSocketConnectionStore
} from './connection-store';
import { extractTokenFromEvent } from './token-from-event';
import { loadWebSocketFunctionEnv } from './websocket-env';
import { WebSocketTokenVerifier } from './websocket-token-verifier';

const env = loadWebSocketFunctionEnv();
const tokenVerifier = new WebSocketTokenVerifier(env);
const connectionStore = new WebSocketConnectionStore(
  env.awsRegion,
  env.connectionsTableName
);

type IWebSocketConnectEvent = Parameters<APIGatewayProxyWebsocketHandlerV2>[0] & {
  queryStringParameters?: Record<string, string | undefined>;
};

/**
 * Handles API Gateway WebSocket `$connect` events.
 *
 * @remarks
 * The handler validates Cognito identity before storing the connection in
 * DynamoDB. It does not bootstrap the Nest HTTP app to keep cold starts small.
 */
export const handler: APIGatewayProxyWebsocketHandlerV2 = async (event) => {
  const connectEvent = event as IWebSocketConnectEvent;
  const connectionId = event.requestContext.connectionId;
  const token = extractTokenFromEvent(event);

  if (!connectionId) {
    return jsonResponse(400, { message: 'Missing connection id' });
  }

  if (!token) {
    return jsonResponse(401, { message: 'Missing authorization token' });
  }

  try {
    const identity = await tokenVerifier.verify(token);

    await connectionStore.store({
      connectionId,
      identity,
      roomId: connectEvent.queryStringParameters?.roomId ?? DEFAULT_WEBSOCKET_ROOM_ID,
      ttlSeconds: env.connectionTtlSeconds
    });

    return jsonResponse(200, { message: 'Connected' });
  } catch {
    return jsonResponse(401, { message: 'Invalid authorization token' });
  }
};
