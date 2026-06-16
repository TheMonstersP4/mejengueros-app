/**
 * Common metadata returned by HTTP API responses.
 */
export interface IApiResponseMeta {
  /**
   * Request identifier used to correlate client reports with logs.
   */
  requestId?: string;

  /**
   * Request path that produced the response.
   */
  path: string;

  /**
   * UTC timestamp generated when the API builds the response.
   */
  timestamp: string;

  /**
   * Optional pagination metadata for list endpoints.
   */
  pagination?: IPaginationMeta;
}

/**
 * Pagination metadata used by list endpoints.
 */
export interface IPaginationMeta {
  /**
   * Current page number.
   */
  page: number;

  /**
   * Number of items requested per page.
   */
  pageSize: number;

  /**
   * Total items available for the query.
   */
  totalItems: number;

  /**
   * Total pages available for the query.
   */
  totalPages: number;
}

/**
 * Safe error item returned to API clients.
 */
export interface IApiErrorItem {
  /**
   * Stable machine-readable error code.
   */
  code: string;

  /**
   * Safe message for API clients.
   */
  message: string;

  /**
   * HTTP status code associated with this error.
   */
  status: number;

  /**
   * Machine-readable problem type or documentation URL.
   */
  type?: string;

  /**
   * Field affected by the error when it is known.
   */
  field?: string;

  /**
   * Safe extra details that help clients recover.
   */
  details?: unknown;
}

/**
 * Standard HTTP API response envelope.
 */
export interface IApiResponse<TData> {
  /**
   * Indicates whether the API operation completed successfully.
   */
  success: boolean;

  /**
   * Main endpoint payload.
   */
  data: TData | null;

  /**
   * Safe errors returned to the client.
   */
  errors: IApiErrorItem[];

  /**
   * Cross-cutting response metadata.
   */
  meta: IApiResponseMeta;
}

/**
 * Internal symbol used to pass endpoint-specific metadata to the global wrapper.
 */
export const API_RESPONSE_META = Symbol('API_RESPONSE_META');

/**
 * Payload shape used when an endpoint needs to provide response metadata.
 */
export interface IApiResponsePayload<TData> {
  /**
   * Marks the payload as carrying explicit API metadata.
   */
  [API_RESPONSE_META]: true;

  /**
   * Main endpoint payload.
   */
  data: TData;

  /**
   * Endpoint-specific metadata merged into the global metadata.
   */
  meta?: Partial<IApiResponseMeta>;
}

/**
 * Wraps endpoint data with metadata for the global response interceptor.
 *
 * @param data - Main endpoint payload.
 * @param meta - Endpoint-specific response metadata.
 * @returns Data and metadata carrier consumed by the interceptor.
 */
export function withApiMeta<TData>(
  data: TData,
  meta: Partial<IApiResponseMeta>
): IApiResponsePayload<TData> {
  return {
    [API_RESPONSE_META]: true,
    data,
    meta
  };
}
