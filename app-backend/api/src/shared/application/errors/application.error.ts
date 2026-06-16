import { BaseError } from '../../domain/errors/base.error';
import type { IBaseErrorProps } from '../../domain/errors/base.error';

/**
 * Base class for expected application-layer errors.
 *
 * @remarks
 * Use this for use-case orchestration failures that are not pure domain
 * invariants and are not provider-specific infrastructure failures.
 */
export abstract class ApplicationError extends BaseError {
  protected constructor(props: IBaseErrorProps) {
    super(props);
  }
}
