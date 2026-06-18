import { applyDecorators, HttpStatus, type Type } from '@nestjs/common';
import {
  ApiExtraModels,
  ApiResponse,
  getSchemaPath
} from '@nestjs/swagger';
import {
  ApiErrorEnvelopeResponse,
  ApiErrorItemResponse,
  ApiResponseMetaResponse,
  ApiSuccessEnvelopeResponse,
  PaginationMetaResponse
} from './api-envelope.dto';

interface IApiEnvelopeResponseOptions {
  /**
   * HTTP status documented for the route response.
   */
  status: number;

  /**
   * Human-readable response description shown in Swagger.
   */
  description: string;

  /**
   * DTO class used as the response envelope data schema.
   */
  type: Type<unknown>;

  /**
   * Whether the data schema is an array of the provided type.
   */
  isArray?: boolean;
}

const errorDescriptions: Record<number, string> = {
  [HttpStatus.BAD_REQUEST]: 'The request body, query, or route parameters are invalid.',
  [HttpStatus.UNAUTHORIZED]: 'The request is missing a valid Cognito bearer token.',
  [HttpStatus.FORBIDDEN]: 'The authenticated user is not allowed to perform this action.',
  [HttpStatus.NOT_FOUND]: 'The requested resource was not found.',
  [HttpStatus.CONFLICT]: 'The request conflicts with the current resource state.',
  [HttpStatus.PAYLOAD_TOO_LARGE]: 'The uploaded payload exceeds the configured limit.',
  [HttpStatus.UNSUPPORTED_MEDIA_TYPE]: 'The uploaded media type is not supported.',
  [HttpStatus.BAD_GATEWAY]: 'A downstream service failed while processing the request.',
  [HttpStatus.INTERNAL_SERVER_ERROR]: 'An unexpected server error occurred.'
};

/**
 * Documents a successful API response wrapped by the global envelope.
 *
 * @param options - Response schema options.
 * @returns Composed Swagger decorators.
 */
export function ApiEnvelopeResponse(
  options: IApiEnvelopeResponseOptions
): MethodDecorator {
  return applyDecorators(
    ApiExtraModels(
      ApiSuccessEnvelopeResponse,
      ApiErrorEnvelopeResponse,
      ApiErrorItemResponse,
      ApiResponseMetaResponse,
      PaginationMetaResponse,
      options.type
    ),
    ApiResponse({
      status: options.status,
      description: options.description,
      schema: {
        allOf: [
          { $ref: getSchemaPath(ApiSuccessEnvelopeResponse) },
          {
            properties: {
              data: buildDataSchema(options.type, options.isArray)
            }
          }
        ]
      }
    })
  );
}

/**
 * Documents a 200 API response wrapped by the global envelope.
 *
 * @param type - DTO class used as the response data schema.
 * @param description - Human-readable response description.
 * @returns Composed Swagger decorators.
 */
export function ApiEnvelopeOk(
  type: Type<unknown>,
  description: string
): MethodDecorator {
  return ApiEnvelopeResponse({
    status: HttpStatus.OK,
    description,
    type
  });
}

/**
 * Documents a 200 API response with an array payload wrapped by the global envelope.
 *
 * @param type - DTO class used as the array item schema.
 * @param description - Human-readable response description.
 * @returns Composed Swagger decorators.
 */
export function ApiEnvelopeArrayOk(
  type: Type<unknown>,
  description: string
): MethodDecorator {
  return ApiEnvelopeResponse({
    status: HttpStatus.OK,
    description,
    type,
    isArray: true
  });
}

/**
 * Documents a 201 API response wrapped by the global envelope.
 *
 * @param type - DTO class used as the response data schema.
 * @param description - Human-readable response description.
 * @returns Composed Swagger decorators.
 */
export function ApiEnvelopeCreated(
  type: Type<unknown>,
  description: string
): MethodDecorator {
  return ApiEnvelopeResponse({
    status: HttpStatus.CREATED,
    description,
    type
  });
}

/**
 * Documents standard API error envelopes for the provided HTTP statuses.
 *
 * @param statuses - HTTP statuses returned by the route on expected failures.
 * @returns Composed Swagger decorators.
 */
export function ApiEnvelopeErrors(...statuses: number[]): MethodDecorator {
  const responses = statuses.map((status) =>
    ApiResponse({
      status,
      description:
        errorDescriptions[status] ?? 'The request failed and returned an error envelope.',
      schema: { $ref: getSchemaPath(ApiErrorEnvelopeResponse) }
    })
  );

  return applyDecorators(
    ApiExtraModels(
      ApiErrorEnvelopeResponse,
      ApiErrorItemResponse,
      ApiResponseMetaResponse,
      PaginationMetaResponse
    ),
    ...responses
  );
}

function buildDataSchema(type: Type<unknown>, isArray = false): object {
  const itemSchema = { $ref: getSchemaPath(type) };

  if (isArray) {
    return {
      type: 'array',
      items: itemSchema
    };
  }

  return itemSchema;
}
