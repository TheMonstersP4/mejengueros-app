import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { DomainError } from '@/shared/domain/errors/domain.error';

export class InvalidReviewEvidenceUploadError extends DomainError {
  private constructor(reason: string, logContext: Record<string, unknown>) {
    super({
      code: APP_ERROR_CODES.VALIDATION_FAILED,
      kind: 'validation',
      userMessage:
        'La imagen seleccionada como evidencia debe ser una carga confirmada del usuario autenticado.',
      internalMessage: reason,
      logContext
    });
  }

  static notFound(imageUploadId: string): InvalidReviewEvidenceUploadError {
    return new InvalidReviewEvidenceUploadError(
      'Review evidence image upload was not found.',
      { imageUploadId, reason: 'not_found' }
    );
  }

  static ownerMismatch(
    imageUploadId: string,
    expectedOwnerSub: string,
    actualOwnerSub: string
  ): InvalidReviewEvidenceUploadError {
    return new InvalidReviewEvidenceUploadError(
      'Review evidence image upload does not belong to the authenticated user.',
      { imageUploadId, expectedOwnerSub, actualOwnerSub, reason: 'owner_mismatch' }
    );
  }

  static invalidPurpose(
    imageUploadId: string,
    actualPurpose: FilePurpose
  ): InvalidReviewEvidenceUploadError {
    return new InvalidReviewEvidenceUploadError(
      'Review evidence image upload has an unsupported purpose.',
      {
        imageUploadId,
        actualPurpose,
        expectedPurpose: FilePurpose.ReviewEvidenceImage,
        reason: 'invalid_purpose'
      }
    );
  }

  static alreadyAssigned(
    imageUploadId: string,
    reviewId?: string
  ): InvalidReviewEvidenceUploadError {
    return new InvalidReviewEvidenceUploadError(
      'Review evidence image upload is already assigned to another review.',
      { imageUploadId, reviewId, reason: 'already_assigned' }
    );
  }
}
