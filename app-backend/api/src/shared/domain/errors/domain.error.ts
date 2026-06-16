import { BaseError } from './base.error';
import type { IBaseErrorProps } from './base.error';

/**
 * Base class for expected domain errors.
 *
 * @remarks
 * Domain errors must stay free of NestJS, HTTP, Prisma, AWS SDK, and Fastify
 * dependencies.
 */
export abstract class DomainError extends BaseError {
  protected constructor(props: IBaseErrorProps) {
    super(props);
  }
}
