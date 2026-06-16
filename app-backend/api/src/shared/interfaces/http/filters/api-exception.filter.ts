import type { ArgumentsHost, ExceptionFilter } from '@nestjs/common';
import { Catch, HttpException, HttpStatus, Inject } from '@nestjs/common';
import type { FastifyReply, FastifyRequest } from 'fastify';
import { PinoLogger } from 'nestjs-pino';
import { APP_ERROR_CODES } from '../../../domain/errors/app-error-code';
import type { IAppErrorCode } from '../../../domain/errors/app-error-code';
import { BaseError, ErrorLogLevel } from '../../../domain/errors/base.error';
import type {
  IErrorKind,
  IErrorLogLevel
} from '../../../domain/errors/base.error';
import type {
  IApiErrorItem,
  IApiResponse,
  IApiResponseMeta
} from '../responses/api-response';

interface INormalizedHttpError {
  /**
   * HTTP status returned to the client.
   */
  status: number;

  /**
   * Safe error items returned to the client.
   */
  errors: IApiErrorItem[];

  /**
   * Log level used for this error.
   */
  logLevel: IErrorLogLevel;

  /**
   * Private diagnostic message written to logs.
   */
  internalMessage: string;

  /**
   * Additional structured context written to logs.
   */
  logContext: Record<string, unknown>;
}

/**
 * Converts thrown HTTP errors into the standard API response envelope.
 */
@Catch()
export class ApiExceptionFilter implements ExceptionFilter {
  constructor(@Inject(PinoLogger) private readonly logger: PinoLogger) {
    this.logger.setContext(ApiExceptionFilter.name);
  }

  /**
   * Handles any exception thrown by an HTTP route.
   *
   * @param exception - Error thrown by a controller, guard, pipe, or use case.
   * @param host - NestJS execution context for the current request.
   */
  catch(exception: unknown, host: ArgumentsHost): void {
    const context = host.switchToHttp();
    const request = context.getRequest<FastifyRequest>();
    const response = context.getResponse<FastifyReply>();
    const normalized = this.normalizeException(exception);

    this.logException(exception, request, normalized);

    response
      .status(normalized.status)
      .type('application/json')
      .send({
        success: false,
        data: null,
        errors: normalized.errors,
        meta: this.createMeta(request)
      } satisfies IApiResponse<null>);
  }

  private normalizeException(exception: unknown): INormalizedHttpError {
    if (exception instanceof BaseError) {
      const status = exception.httpStatus ?? this.statusFromKind(exception.kind);

      return {
        status,
        errors: [
          {
            code: exception.code,
            message: exception.userMessage,
            status,
            type: this.problemType(exception.code)
          }
        ],
        logLevel: status >= 500 ? ErrorLogLevel.Error : exception.logLevel,
        internalMessage: exception.internalMessage,
        logContext: exception.logContext
      };
    }

    if (exception instanceof HttpException) {
      return this.normalizeHttpException(exception);
    }

    return {
      status: HttpStatus.INTERNAL_SERVER_ERROR,
      errors: [
        {
          code: APP_ERROR_CODES.INTERNAL_SERVER_ERROR,
          message: 'An unexpected error occurred.',
          status: HttpStatus.INTERNAL_SERVER_ERROR,
          type: this.problemType(APP_ERROR_CODES.INTERNAL_SERVER_ERROR)
        }
      ],
      logLevel: ErrorLogLevel.Error,
      internalMessage:
        exception instanceof Error ? exception.message : 'Unknown exception',
      logContext: {}
    };
  }

  private normalizeHttpException(
    exception: HttpException
  ): INormalizedHttpError {
    const status = exception.getStatus();
    const response = exception.getResponse();
    const details = this.readHttpExceptionDetails(response);
    const code = this.codeFromHttpStatus(status);

    return {
      status,
      errors: details.messages.map((message) => ({
        code,
        message,
        status,
        type: this.problemType(code),
        details: details.details
      })),
      logLevel: status >= 500 ? ErrorLogLevel.Error : ErrorLogLevel.Warn,
      internalMessage: exception.message,
      logContext: {}
    };
  }

  private readHttpExceptionDetails(response: string | object): {
    messages: string[];
    details?: unknown;
  } {
    if (typeof response === 'string') {
      return { messages: [response] };
    }

    const body = response as {
      message?: unknown;
    };

    if (Array.isArray(body.message)) {
      const messages = body.message.filter(
        (message): message is string => typeof message === 'string'
      );

      return {
        messages: messages.length > 0 ? messages : ['Request validation failed.'],
        details: body.message
      };
    }

    return {
      messages: [
        typeof body.message === 'string'
          ? body.message
          : 'Request could not be processed.'
      ]
    };
  }

  private createMeta(request: FastifyRequest): IApiResponseMeta {
    return {
      requestId: this.readRequestId(request),
      path: request.url,
      timestamp: new Date().toISOString()
    };
  }

  private logException(
    exception: unknown,
    request: FastifyRequest,
    normalized: INormalizedHttpError
  ): void {
    const primaryError = normalized.errors[0];
    const payload = {
      method: request.method,
      url: request.url,
      status: normalized.status,
      code: primaryError?.code,
      traceId: this.readRequestId(request),
      internalMessage: normalized.internalMessage,
      context: normalized.logContext,
      stack: exception instanceof Error ? exception.stack : undefined
    };

    if (normalized.logLevel === ErrorLogLevel.Error) {
      this.logger.error(payload, normalized.internalMessage);
      return;
    }

    if (normalized.logLevel === ErrorLogLevel.Warn) {
      this.logger.warn(payload, normalized.internalMessage);
      return;
    }

    if (normalized.logLevel === ErrorLogLevel.Info) {
      this.logger.info(payload, normalized.internalMessage);
      return;
    }

    this.logger.debug(payload, normalized.internalMessage);
  }

  private statusFromKind(kind: IErrorKind): number {
    const statusByKind: Record<IErrorKind, number> = {
      auth: HttpStatus.UNAUTHORIZED,
      conflict: HttpStatus.CONFLICT,
      external: HttpStatus.BAD_GATEWAY,
      forbidden: HttpStatus.FORBIDDEN,
      internal: HttpStatus.INTERNAL_SERVER_ERROR,
      not_found: HttpStatus.NOT_FOUND,
      validation: HttpStatus.BAD_REQUEST
    };

    return statusByKind[kind];
  }

  private codeFromHttpStatus(status: number): IAppErrorCode {
    const codeByStatus: Record<number, IAppErrorCode> = {
      [HttpStatus.BAD_REQUEST]: APP_ERROR_CODES.VALIDATION_FAILED,
      [HttpStatus.UNAUTHORIZED]: APP_ERROR_CODES.AUTH_INVALID_TOKEN,
      [HttpStatus.FORBIDDEN]: APP_ERROR_CODES.FORBIDDEN,
      [HttpStatus.NOT_FOUND]: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      [HttpStatus.METHOD_NOT_ALLOWED]: APP_ERROR_CODES.METHOD_NOT_ALLOWED,
      [HttpStatus.CONFLICT]: APP_ERROR_CODES.CONFLICT,
      [HttpStatus.PAYLOAD_TOO_LARGE]: APP_ERROR_CODES.PAYLOAD_TOO_LARGE,
      [HttpStatus.UNSUPPORTED_MEDIA_TYPE]: APP_ERROR_CODES.UNSUPPORTED_MEDIA_TYPE,
      [HttpStatus.TOO_MANY_REQUESTS]: APP_ERROR_CODES.RATE_LIMITED,
      [HttpStatus.BAD_GATEWAY]: APP_ERROR_CODES.EXTERNAL_SERVICE_ERROR,
      [HttpStatus.SERVICE_UNAVAILABLE]: APP_ERROR_CODES.SERVICE_UNAVAILABLE,
      [HttpStatus.GATEWAY_TIMEOUT]: APP_ERROR_CODES.GATEWAY_TIMEOUT
    };

    return codeByStatus[status] ?? APP_ERROR_CODES.INTERNAL_SERVER_ERROR;
  }

  private problemType(code: IAppErrorCode): string {
    const slug = code.toLowerCase().replaceAll('_', '-');
    const baseUrl = process.env.ERROR_DOCUMENTATION_BASE_URL;

    if (baseUrl) {
      return `${baseUrl.replace(/\/$/, '')}/${slug}`;
    }

    return `urn:problem-type:backend:${slug}`;
  }

  private readRequestId(request: FastifyRequest): string | undefined {
    return typeof request.id === 'string' ? request.id : undefined;
  }
}
