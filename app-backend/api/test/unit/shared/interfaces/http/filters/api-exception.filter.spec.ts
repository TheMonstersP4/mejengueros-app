import type { ArgumentsHost } from '@nestjs/common';
import { BadRequestException, HttpException, HttpStatus } from '@nestjs/common';
import type { FastifyReply, FastifyRequest } from 'fastify';
import type { PinoLogger } from 'nestjs-pino';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { BaseError, ErrorLogLevel } from '@/shared/domain/errors/base.error';
import type { IErrorKind } from '@/shared/domain/errors/base.error';
import { ApiExceptionFilter } from '@/shared/interfaces/http/filters/api-exception.filter';

class ExpectedError extends BaseError {
  constructor(
    kind: IErrorKind,
    logLevel: ErrorLogLevel = ErrorLogLevel.Warn,
    httpStatus?: number
  ) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind,
      userMessage: 'Safe validation message.',
      internalMessage: 'Internal validation message.',
      logContext: { field: 'email' },
      logLevel,
      httpStatus
    });
  }
}

describe('ApiExceptionFilter', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  function createFilter() {
    const logger = {
      setContext: jest.fn(),
      error: jest.fn(),
      warn: jest.fn(),
      info: jest.fn(),
      debug: jest.fn()
    } as unknown as jest.Mocked<PinoLogger>;
    const filter = new ApiExceptionFilter(logger);

    return { filter, logger };
  }

  function createHost(path = '/v1/test') {
    const request = {
      method: 'GET',
      url: path,
      id: 'trace-1'
    } as FastifyRequest;
    const response = {
      status: jest.fn().mockReturnThis(),
      type: jest.fn().mockReturnThis(),
      send: jest.fn()
    } as unknown as jest.Mocked<FastifyReply>;
    const host = {
      switchToHttp: () => ({
        getRequest: () => request,
        getResponse: () => response
      })
    } as ArgumentsHost;

    return { host, request, response };
  }

  it('sets logger context on construction', () => {
    const { logger } = createFilter();

    expect(logger.setContext).toHaveBeenCalledWith(ApiExceptionFilter.name);
  });

  it.each([
    ['auth', HttpStatus.UNAUTHORIZED],
    ['conflict', HttpStatus.CONFLICT],
    ['external', HttpStatus.BAD_GATEWAY],
    ['forbidden', HttpStatus.FORBIDDEN],
    ['internal', HttpStatus.INTERNAL_SERVER_ERROR],
    ['not_found', HttpStatus.NOT_FOUND],
    ['validation', HttpStatus.BAD_REQUEST]
  ] as const)('maps %s errors to HTTP status %i', (kind, status) => {
    process.env.ERROR_DOCUMENTATION_BASE_URL = 'https://errors.example.test/';
    const { filter } = createFilter();
    const { host, response } = createHost('/v1/resource');

    filter.catch(new ExpectedError(kind), host);

    expect(response.status).toHaveBeenCalledWith(status);
    expect(response.type).toHaveBeenCalledWith('application/json');
    expect(response.send).toHaveBeenCalledWith({
      success: false,
      data: null,
      errors: [
        {
          code: APP_ERROR_CODES.VALIDATION_FAILED,
          message: 'Safe validation message.',
          status,
          type: 'https://errors.example.test/validation-failed'
        }
      ],
      meta: expect.objectContaining({
        requestId: 'trace-1',
        path: '/v1/resource',
        timestamp: expect.any(String)
      })
    });
  });

  it('uses explicit HTTP status overrides from expected errors', () => {
    const { filter } = createFilter();
    const { host, response } = createHost();

    filter.catch(new ExpectedError('validation', ErrorLogLevel.Warn, 415), host);

    expect(response.status).toHaveBeenCalledWith(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    expect(response.send).toHaveBeenCalledWith(
      expect.objectContaining({
        errors: [
          expect.objectContaining({
            status: HttpStatus.UNSUPPORTED_MEDIA_TYPE
          })
        ]
      })
    );
  });

  it.each([
    [ErrorLogLevel.Debug, 'debug'],
    [ErrorLogLevel.Info, 'info'],
    [ErrorLogLevel.Warn, 'warn']
  ] as const)('logs expected client errors at %s level', (level, method) => {
    const { filter, logger } = createFilter();
    const { host } = createHost();

    filter.catch(new ExpectedError('validation', level), host);

    expect(logger[method]).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'GET',
        url: '/v1/test',
        status: HttpStatus.BAD_REQUEST,
        code: APP_ERROR_CODES.VALIDATION_FAILED,
        traceId: 'trace-1',
        internalMessage: 'Internal validation message.',
        context: { field: 'email' }
      }),
      'Internal validation message.'
    );
  });

  it('normalizes validation HttpExceptions into error arrays', () => {
    const { filter, logger } = createFilter();
    const { host, response } = createHost();

    filter.catch(new BadRequestException(['email must be valid']), host);

    expect(response.send).toHaveBeenCalledWith(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            message: 'email must be valid',
            status: HttpStatus.BAD_REQUEST,
            details: ['email must be valid']
          })
        ]
      })
    );
    expect(logger.warn).toHaveBeenCalledWith(
      expect.objectContaining({
        internalMessage: 'Bad Request Exception'
      }),
      'Bad Request Exception'
    );
  });

  it('uses fallback validation messages when HTTP exception arrays have no strings', () => {
    const { filter } = createFilter();
    const { host, response } = createHost();

    filter.catch(
      new HttpException({ message: [123] }, HttpStatus.BAD_REQUEST),
      host
    );

    expect(response.send).toHaveBeenCalledWith(
      expect.objectContaining({
        errors: [
          expect.objectContaining({
            message: 'Request validation failed.',
            details: [123]
          })
        ]
      })
    );
  });

  it('uses fallback messages for HTTP exception objects without string messages', () => {
    const { filter } = createFilter();
    const { host, response } = createHost();

    filter.catch(new HttpException({ message: 123 }, 418), host);

    expect(response.send).toHaveBeenCalledWith(
      expect.objectContaining({
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.INTERNAL_SERVER_ERROR,
            message: 'Request could not be processed.',
            status: 418
          })
        ]
      })
    );
  });

  it.each([
    [HttpStatus.FORBIDDEN, APP_ERROR_CODES.FORBIDDEN],
    [HttpStatus.NOT_FOUND, APP_ERROR_CODES.RESOURCE_NOT_FOUND],
    [HttpStatus.PAYLOAD_TOO_LARGE, APP_ERROR_CODES.PAYLOAD_TOO_LARGE],
    [
      HttpStatus.UNSUPPORTED_MEDIA_TYPE,
      APP_ERROR_CODES.UNSUPPORTED_MEDIA_TYPE
    ],
    [HttpStatus.BAD_GATEWAY, APP_ERROR_CODES.EXTERNAL_SERVICE_ERROR]
  ])('maps HTTP status %i to application code %s', (status, code) => {
    const { filter } = createFilter();
    const { host, response } = createHost();

    filter.catch(new HttpException('http error', status), host);

    expect(response.send).toHaveBeenCalledWith(
      expect.objectContaining({
        errors: [
          expect.objectContaining({
            status,
            code
          })
        ]
      })
    );
  });

  it('normalizes unknown errors as internal server errors', () => {
    const { filter, logger } = createFilter();
    const { host, response } = createHost();
    const error = new Error('database down');

    filter.catch(error, host);

    expect(response.status).toHaveBeenCalledWith(HttpStatus.INTERNAL_SERVER_ERROR);
    expect(response.send).toHaveBeenCalledWith(
      expect.objectContaining({
        errors: [
          {
            code: APP_ERROR_CODES.INTERNAL_SERVER_ERROR,
            message: 'An unexpected error occurred.',
            status: HttpStatus.INTERNAL_SERVER_ERROR,
            type: 'urn:problem-type:backend:internal-server-error'
          }
        ]
      })
    );
    expect(logger.error).toHaveBeenCalledWith(
      expect.objectContaining({
        internalMessage: 'database down',
        stack: error.stack
      }),
      'database down'
    );
  });
});
