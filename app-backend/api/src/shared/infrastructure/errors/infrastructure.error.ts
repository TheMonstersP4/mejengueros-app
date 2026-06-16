import { BaseError } from '../../domain/errors/base.error';
import type { IBaseErrorProps } from '../../domain/errors/base.error';

/**
 * Base class for expected infrastructure errors.
 *
 * @remarks
 * Use this when wrapping database, cloud provider, HTTP client, queue, storage,
 * or other adapter failures before they cross into application code.
 */
export abstract class InfrastructureError extends BaseError {
  protected constructor(props: IBaseErrorProps) {
    super(props);
  }
}
