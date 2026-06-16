/**
 * Stable machine-readable error codes exposed by the API.
 *
 * @remarks
 * These values are part of the client contract. Do not rename or reuse a code
 * with a different meaning once it has been released.
 */
export const APP_ERROR_CODES = {
  AUTH_INVALID_TOKEN: 'AUTH_INVALID_TOKEN',
  AUTH_MISSING_TOKEN: 'AUTH_MISSING_TOKEN',
  BAD_REQUEST: 'BAD_REQUEST',
  CONFLICT: 'CONFLICT',
  EXTERNAL_SERVICE_ERROR: 'EXTERNAL_SERVICE_ERROR',
  FORBIDDEN: 'FORBIDDEN',
  GATEWAY_TIMEOUT: 'GATEWAY_TIMEOUT',
  INTERNAL_SERVER_ERROR: 'INTERNAL_SERVER_ERROR',
  METHOD_NOT_ALLOWED: 'METHOD_NOT_ALLOWED',
  PAYLOAD_TOO_LARGE: 'PAYLOAD_TOO_LARGE',
  RATE_LIMITED: 'RATE_LIMITED',
  RESOURCE_NOT_FOUND: 'RESOURCE_NOT_FOUND',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
  UNSUPPORTED_MEDIA_TYPE: 'UNSUPPORTED_MEDIA_TYPE',
  VALIDATION_FAILED: 'VALIDATION_FAILED'
} as const;

/**
 * Error code values accepted by the application error system.
 */
export type IAppErrorCode =
  (typeof APP_ERROR_CODES)[keyof typeof APP_ERROR_CODES];
