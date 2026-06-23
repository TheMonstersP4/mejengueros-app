import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an email belongs to an existing user but cannot be linked safely.
 */
export class UserEmailAlreadyExistsError extends DomainError {
  constructor(email: string, providers: string[]) {
    const providerList = providers.length > 0 ? providers.join(', ') : 'another provider';

    super({
      code: APP_ERROR_CODES.USER_EMAIL_ALREADY_EXISTS,
      kind: 'conflict',
      userMessage: `Este correo ya está registrado con ${providerList}. Inicia sesión con el proveedor vinculado para continuar.`,
      internalMessage:
        'Email already belongs to a user and cannot be linked without verified ownership.',
      logContext: { email, providers }
    });

    this.name = 'UserEmailAlreadyExistsError';
  }
}
