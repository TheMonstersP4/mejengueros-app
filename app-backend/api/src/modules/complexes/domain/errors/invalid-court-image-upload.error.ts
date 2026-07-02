import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { APP_ERROR_CODES } from '../../../../shared/domain/errors/app-error-code';
import { DomainError } from '../../../../shared/domain/errors/domain.error';

export class InvalidCourtImageUploadError extends DomainError {
  private constructor(reason: string, logContext: Record<string, unknown>) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage:
        'La imagen seleccionada para la cancha debe ser una carga confirmada del usuario autenticado.',
      internalMessage: reason,
      logContext
    });
  }

  static notFound(imageUploadId: string): InvalidCourtImageUploadError {
    return new InvalidCourtImageUploadError('Court image upload was not found.', {
      imageUploadId,
      reason: 'not_found'
    });
  }

  static ownerMismatch(
    imageUploadId: string,
    expectedOwnerSub: string,
    actualOwnerSub: string
  ): InvalidCourtImageUploadError {
    return new InvalidCourtImageUploadError(
      'Court image upload does not belong to the authenticated owner.',
      {
        imageUploadId,
        expectedOwnerSub,
        actualOwnerSub,
        reason: 'owner_mismatch'
      }
    );
  }

  static invalidPurpose(
    imageUploadId: string,
    actualPurpose: FilePurpose
  ): InvalidCourtImageUploadError {
    return new InvalidCourtImageUploadError(
      'Court image upload has an unsupported purpose.',
      {
        imageUploadId,
        actualPurpose,
        expectedPurpose: FilePurpose.CourtImage,
        reason: 'invalid_purpose'
      }
    );
  }

  static alreadyAssigned(
    imageUploadId: string,
    courtId?: string
  ): InvalidCourtImageUploadError {
    return new InvalidCourtImageUploadError(
      'Court image upload is already assigned to another court.',
      {
        imageUploadId,
        courtId,
        reason: 'already_assigned'
      }
    );
  }
}
