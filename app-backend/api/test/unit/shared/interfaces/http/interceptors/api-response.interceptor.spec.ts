import type { CallHandler, ExecutionContext } from '@nestjs/common';
import type { FastifyRequest } from 'fastify';
import { of } from 'rxjs';
import { ApiResponseInterceptor } from '@/shared/interfaces/http/interceptors/api-response.interceptor';
import { withApiMeta } from '@/shared/interfaces/http/responses/api-response';

describe('ApiResponseInterceptor', () => {
  function createContext(path = '/v1/health'): ExecutionContext {
    const request = {
      url: path,
      id: 'request-id'
    } as FastifyRequest;

    return {
      switchToHttp: () => ({
        getRequest: () => request
      })
    } as ExecutionContext;
  }

  function createContextWithoutRequestId(path = '/v1/health'): ExecutionContext {
    const request = {
      url: path,
      id: 123
    } as unknown as FastifyRequest;

    return {
      switchToHttp: () => ({
        getRequest: () => request
      })
    } as ExecutionContext;
  }

  it('wraps successful route responses in the standard envelope', (done) => {
    const interceptor = new ApiResponseInterceptor();
    const next = {
      handle: () => of({ status: 'ok' })
    } as CallHandler;

    interceptor.intercept(createContext(), next).subscribe((response) => {
      expect(response).toEqual({
        success: true,
        data: { status: 'ok' },
        errors: [],
        meta: {
          requestId: 'request-id',
          path: '/v1/health',
          timestamp: expect.any(String)
        }
      });
      done();
    });
  });

  it('merges endpoint-specific metadata', (done) => {
    const interceptor = new ApiResponseInterceptor();
    const next = {
      handle: () =>
        of(
          withApiMeta([{ id: 'court-1' }], {
            pagination: {
              page: 1,
              pageSize: 20,
              totalItems: 1,
              totalPages: 1
            }
          })
        )
    } as CallHandler;

    interceptor.intercept(createContext('/v1/courts'), next).subscribe((response) => {
      expect(response.meta.pagination).toEqual({
        page: 1,
        pageSize: 20,
        totalItems: 1,
        totalPages: 1
      });
      expect(response.data).toEqual([{ id: 'court-1' }]);
      done();
    });
  });

  it('does not wrap responses that are already in the API envelope', (done) => {
    const interceptor = new ApiResponseInterceptor();
    const envelope = {
      success: true,
      data: { already: true },
      errors: [],
      meta: {
        path: '/v1/custom',
        timestamp: '2026-06-05T00:00:00.000Z'
      }
    };
    const next = {
      handle: () => of(envelope)
    } as CallHandler;

    interceptor.intercept(createContext('/v1/custom'), next).subscribe((response) => {
      expect(response).toBe(envelope);
      done();
    });
  });

  it('wraps nullish controller results as null data without request id', (done) => {
    const interceptor = new ApiResponseInterceptor();
    const next = {
      handle: () => of(undefined)
    } as CallHandler;

    interceptor
      .intercept(createContextWithoutRequestId('/v1/empty'), next)
      .subscribe((response) => {
        expect(response).toEqual({
          success: true,
          data: null,
          errors: [],
          meta: {
            requestId: undefined,
            path: '/v1/empty',
            timestamp: expect.any(String)
          }
        });
        done();
      });
  });
});
