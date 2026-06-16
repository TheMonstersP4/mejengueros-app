import type {
  CallHandler,
  ExecutionContext,
  NestInterceptor
} from '@nestjs/common';
import { Injectable } from '@nestjs/common';
import type { FastifyRequest } from 'fastify';
import type { Observable } from 'rxjs';
import { map } from 'rxjs';
import {
  API_RESPONSE_META,
  type IApiResponse,
  type IApiResponseMeta,
  type IApiResponsePayload
} from '../responses/api-response';

/**
 * Wraps successful HTTP responses in the API response envelope.
 */
@Injectable()
export class ApiResponseInterceptor implements NestInterceptor {
  /**
   * Wraps controller return values after route execution.
   *
   * @param context - NestJS execution context for the current request.
   * @param next - Route handler pipeline.
   * @returns Observable that emits a standardized API response.
   */
  intercept(
    context: ExecutionContext,
    next: CallHandler
  ): Observable<IApiResponse<unknown>> {
    const request = context.switchToHttp().getRequest<FastifyRequest>();

    return next.handle().pipe(
      map((payload: unknown) => {
        if (this.isApiResponse(payload)) {
          return payload;
        }

        const carried = this.readCarriedPayload(payload);
        const meta = this.createMeta(request, carried.meta);

        return {
          success: true,
          data: carried.data,
          errors: [],
          meta
        };
      })
    );
  }

  private readCarriedPayload(payload: unknown): {
    data: unknown;
    meta?: Partial<IApiResponseMeta>;
  } {
    if (this.isApiResponsePayload(payload)) {
      return {
        data: payload.data,
        meta: payload.meta
      };
    }

    return { data: payload ?? null };
  }

  private createMeta(
    request: FastifyRequest,
    extra?: Partial<IApiResponseMeta>
  ): IApiResponseMeta {
    return {
      requestId: this.readRequestId(request),
      path: request.url,
      timestamp: new Date().toISOString(),
      ...extra
    };
  }

  private isApiResponse(payload: unknown): payload is IApiResponse<unknown> {
    if (!payload || typeof payload !== 'object') {
      return false;
    }

    const value = payload as Partial<IApiResponse<unknown>>;
    return (
      typeof value.success === 'boolean' &&
      'data' in value &&
      Array.isArray(value.errors) &&
      typeof value.meta === 'object'
    );
  }

  private isApiResponsePayload(
    payload: unknown
  ): payload is IApiResponsePayload<unknown> {
    return Boolean(
      payload &&
        typeof payload === 'object' &&
        (payload as Record<symbol, unknown>)[API_RESPONSE_META] === true
    );
  }

  private readRequestId(request: FastifyRequest): string | undefined {
    return typeof request.id === 'string' ? request.id : undefined;
  }
}
