import type { APIGatewayProxyStructuredResultV2 } from 'aws-lambda';

/**
 * Builds a JSON Lambda proxy response.
 *
 * @param statusCode - HTTP status code returned to API Gateway.
 * @param body - JSON-serializable response body.
 * @returns API Gateway structured response.
 */
export function jsonResponse(
  statusCode: number,
  body: Record<string, unknown>
): APIGatewayProxyStructuredResultV2 {
  return {
    statusCode,
    headers: {
      'content-type': 'application/json'
    },
    body: JSON.stringify(body)
  };
}
