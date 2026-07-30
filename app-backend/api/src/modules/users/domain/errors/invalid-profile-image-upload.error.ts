import { FilePurpose } from '../../../files/domain/enums/file-purpose.enum';
import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import type { IAppErrorCode } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

/**
 * Raised when an upload cannot be associated as the current user's profile image.
 */
export class InvalidProfileImageUploadError extends DomainError {
  private constructor(props: {
    code: IAppErrorCode;
    kind: 'conflict' | 'forbidden' | 'not_found' | 'validation';
    userMessage: string;
    internalMessage: string;
    logContext: Record<string, unknown>;
  }) {
    super(props);
  }

  static notFound(imageUploadId: string): InvalidProfileImageUploadError {
    return new InvalidProfileImageUploadError({
      code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
      kind: 'not_found',
      userMessage: 'The selected profile image upload was not found.',
      internalMessage: 'Profile image upload was not found.',
      logContext: { imageUploadId, reason: 'not_found' }
    });
  }

  static ownerMismatch(imageUploadId: string): InvalidProfileImageUploadError {
    return new InvalidProfileImageUploadError({
      code: APP_ERROR_CODES.FORBIDDEN,
      kind: 'forbidden',
      userMessage: 'The selected profile image does not belong to the current user.',
      internalMessage: 'Profile image upload owner does not match the authenticated user.',
      logContext: { imageUploadId, reason: 'owner_mismatch' }
    });
  }

  static invalidPurpose(
    imageUploadId: string,
    actualPurpose: FilePurpose
  ): InvalidProfileImageUploadError {
    return new InvalidProfileImageUploadError({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage: 'The selected upload is not a profile image.',
      internalMessage: 'Profile image upload has an unsupported purpose.',
      logContext: {
        imageUploadId,
        actualPurpose,
        expectedPurpose: FilePurpose.ProfileImage,
        reason: 'invalid_purpose'
      }
    });
  }

  static alreadyAssigned(imageUploadId: string): InvalidProfileImageUploadError {
    return new InvalidProfileImageUploadError({
      code: APP_ERROR_CODES.CONFLICT,
      kind: 'conflict',
      userMessage: 'The selected profile image is already assigned to another user.',
      internalMessage: 'Profile image upload unique relation is already assigned.',
      logContext: { imageUploadId, reason: 'already_assigned' }
    });
  }
}
