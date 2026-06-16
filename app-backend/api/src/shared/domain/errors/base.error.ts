import type { IAppErrorCode } from './app-error-code';

/**
 * Transport-neutral category used to map application errors to delivery-layer
 * responses such as HTTP Problem Details.
 */
export type IErrorKind =
  | 'auth'
  | 'conflict'
  | 'external'
  | 'forbidden'
  | 'internal'
  | 'not_found'
  | 'validation';

/**
 * Log level requested by an expected application error.
 */
export enum ErrorLogLevel {
  Debug = 'debug',
  Info = 'info',
  Warn = 'warn',
  Error = 'error'
}

/**
 * Error log level values accepted by expected application errors.
 */
export type IErrorLogLevel = ErrorLogLevel;

/**
 * Metadata required to create an expected application error.
 */
export interface IBaseErrorProps {
  /**
   * Stable machine-readable code exposed to API clients.
   */
  code: IAppErrorCode;

  /**
   * Transport-neutral category used by boundary adapters.
   */
  kind: IErrorKind;

  /**
   * Safe message that can be returned to end users.
   */
  userMessage: string;

  /**
   * Private diagnostic message written to logs.
   */
  internalMessage?: string;

  /**
   * Structured context for logs. Do not include secrets or tokens.
   */
  logContext?: Record<string, unknown>;

  /**
   * Preferred log level for this expected error.
   */
  logLevel?: IErrorLogLevel;

  /**
   * Optional HTTP status override used only by delivery boundaries.
   */
  httpStatus?: number;

  /**
   * Original error when wrapping provider, database, or SDK failures.
   */
  cause?: unknown;
}

/**
 * Base class for all expected application errors.
 *
 * @remarks
 * Domain, application, and infrastructure errors extend this class through a
 * layer-specific base class. Controllers should not catch these errors; the
 * delivery boundary maps them to a response format.
 */
export abstract class BaseError extends Error {
  /**
   * Stable code clients can branch on.
   */
  readonly code: IAppErrorCode;

  /**
   * Category used to map the error to transport status.
   */
  readonly kind: IErrorKind;

  /**
   * Safe message for users and API clients.
   */
  readonly userMessage: string;

  /**
   * Private message for logs and observability.
   */
  readonly internalMessage: string;

  /**
   * Structured context for logs.
   */
  readonly logContext: Record<string, unknown>;

  /**
   * Suggested log severity.
   */
  readonly logLevel: IErrorLogLevel;

  /**
   * Optional HTTP status override used by transport adapters.
   */
  readonly httpStatus?: number;

  /**
   * Marks the error as expected and safe to normalize.
   */
  readonly isOperational = true;

  protected constructor(props: IBaseErrorProps) {
    super(props.internalMessage ?? props.userMessage, { cause: props.cause });

    this.code = props.code;
    this.kind = props.kind;
    this.userMessage = props.userMessage;
    this.internalMessage = props.internalMessage ?? props.userMessage;
    this.logContext = props.logContext ?? {};
    this.logLevel = props.logLevel ?? ErrorLogLevel.Warn;
    this.httpStatus = props.httpStatus;
  }
}
