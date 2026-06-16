import type { APIGatewayProxyWebsocketEventV2 } from 'aws-lambda';

type IWebSocketAuthEvent = APIGatewayProxyWebsocketEventV2 & {
  headers?: Record<string, string | undefined>;
  queryStringParameters?: Record<string, string | undefined>;
};

/**
 * Extracts a Cognito token from a WebSocket connect event.
 *
 * @remarks
 * API Gateway WebSocket clients commonly send auth data in the query string
 * during `$connect`, but this helper also accepts an Authorization header for
 * local tests and compatible clients.
 *
 * @param event - API Gateway WebSocket event.
 * @returns Token without the `Bearer` prefix, or `undefined`.
 */
export function extractTokenFromEvent(
  event: IWebSocketAuthEvent
): string | undefined {
  const headerToken =
    event.headers?.authorization ?? event.headers?.Authorization;
  const queryToken =
    event.queryStringParameters?.token ??
    event.queryStringParameters?.authorization ??
    event.queryStringParameters?.Authorization;

  return normalizeBearerToken(headerToken ?? queryToken);
}

function normalizeBearerToken(value: string | undefined): string | undefined {
  if (!value) {
    return undefined;
  }

  return value.startsWith('Bearer ') ? value.slice('Bearer '.length) : value;
}
