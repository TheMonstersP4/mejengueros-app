import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

/**
 * Pagination metadata returned by paginated endpoints.
 */
export class PaginationMetaResponse {
  /**
   * Current page number.
   */
  @ApiProperty({ example: 1 })
  page!: number;

  /**
   * Number of items requested per page.
   */
  @ApiProperty({ example: 20 })
  pageSize!: number;

  /**
   * Total items available for the query.
   */
  @ApiProperty({ example: 100 })
  totalItems!: number;

  /**
   * Total pages available for the query.
   */
  @ApiProperty({ example: 5 })
  totalPages!: number;
}

/**
 * Metadata included in every API response envelope.
 */
export class ApiResponseMetaResponse {
  /**
   * Request identifier used to correlate client reports with logs.
   */
  @ApiPropertyOptional({ example: 'req-01HZY7ZJ7F3YB5X9M7P5H5B9H8' })
  requestId?: string;

  /**
   * Request path that produced the response.
   */
  @ApiProperty({ example: '/v1/users/me' })
  path!: string;

  /**
   * UTC timestamp generated when the API builds the response.
   */
  @ApiProperty({ example: '2026-06-17T18:20:00.000Z' })
  timestamp!: string;

  /**
   * Pagination metadata for list endpoints when pagination is enabled.
   */
  @ApiPropertyOptional({ type: () => PaginationMetaResponse })
  pagination?: PaginationMetaResponse;
}

/**
 * Safe error item returned to API clients.
 */
export class ApiErrorItemResponse {
  /**
   * Stable machine-readable error code.
   */
  @ApiProperty({ example: 'AUTH_MISSING_TOKEN' })
  code!: string;

  /**
   * Safe message for API clients.
   */
  @ApiProperty({ example: 'Authentication token is required.' })
  message!: string;

  /**
   * HTTP status code associated with this error.
   */
  @ApiProperty({ example: 401 })
  status!: number;

  /**
   * Machine-readable problem type or documentation URL.
   */
  @ApiPropertyOptional({
    example: 'urn:problem-type:backend:auth-missing-token'
  })
  type?: string;

  /**
   * Field affected by the error when it is known.
   */
  @ApiPropertyOptional({ example: 'contentType' })
  field?: string;

  /**
   * Safe extra details that help clients recover.
   */
  @ApiPropertyOptional({ example: ['contentType must be a MIME type'] })
  details?: unknown;
}

/**
 * Standard success envelope returned by the API.
 */
export class ApiSuccessEnvelopeResponse {
  /**
   * Indicates that the operation completed successfully.
   */
  @ApiProperty({ example: true })
  success!: true;

  /**
   * Endpoint payload. The concrete schema is provided by each route.
   */
  @ApiProperty({ nullable: true })
  data!: unknown;

  /**
   * Empty error list for successful responses.
   */
  @ApiProperty({ type: [ApiErrorItemResponse], example: [] })
  errors!: ApiErrorItemResponse[];

  /**
   * Cross-cutting response metadata.
   */
  @ApiProperty({ type: () => ApiResponseMetaResponse })
  meta!: ApiResponseMetaResponse;
}

/**
 * Standard error envelope returned by the API.
 */
export class ApiErrorEnvelopeResponse {
  /**
   * Indicates that the operation failed.
   */
  @ApiProperty({ example: false })
  success!: false;

  /**
   * Error responses never include endpoint data.
   */
  @ApiProperty({ nullable: true, example: null })
  data!: null;

  /**
   * Safe errors returned to the client.
   */
  @ApiProperty({ type: [ApiErrorItemResponse] })
  errors!: ApiErrorItemResponse[];

  /**
   * Cross-cutting response metadata.
   */
  @ApiProperty({ type: () => ApiResponseMetaResponse })
  meta!: ApiResponseMetaResponse;
}
