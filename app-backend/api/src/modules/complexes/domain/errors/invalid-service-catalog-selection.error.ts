import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when wizard service selections are missing, inactive, or from the wrong scope.
 */
export class InvalidServiceCatalogSelectionError extends DomainError {
  constructor(target: 'complex' | 'court', scope: 'COMPLEX' | 'COURT', serviceIds: string[]) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: `Selected ${target} services must exist, be active, and belong to scope ${scope}.`,
      logContext: {
        target,
        scope,
        serviceIds
      }
    });
  }
}
