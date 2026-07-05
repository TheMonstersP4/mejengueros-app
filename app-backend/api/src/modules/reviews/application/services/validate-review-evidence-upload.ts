import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import { InvalidReviewEvidenceUploadError } from '../../domain/errors/invalid-review-evidence-upload.error';
import type { IReviewRepository } from '../../domain/repositories/review.repository';

export async function validateReviewEvidenceUpload(
  reviewRepository: Pick<IReviewRepository, 'findReviewIdByEvidenceImageUploadId'>,
  imageUploadRepository: IImageUploadRepository,
  ownerSub: string,
  imageUploadId?: string
): Promise<void> {
  if (imageUploadId == null) {
    return;
  }

  const imageUpload = await imageUploadRepository.findById(imageUploadId);

  if (imageUpload == null) {
    throw InvalidReviewEvidenceUploadError.notFound(imageUploadId);
  }

  const snapshot = imageUpload.toSnapshot();

  if (snapshot.ownerSub !== ownerSub) {
    throw InvalidReviewEvidenceUploadError.ownerMismatch(
      imageUploadId,
      ownerSub,
      snapshot.ownerSub
    );
  }

  if (snapshot.purpose !== FilePurpose.ReviewEvidenceImage) {
    throw InvalidReviewEvidenceUploadError.invalidPurpose(
      imageUploadId,
      snapshot.purpose
    );
  }

  const existingReviewId =
    await reviewRepository.findReviewIdByEvidenceImageUploadId(imageUploadId);

  if (existingReviewId != null) {
    throw InvalidReviewEvidenceUploadError.alreadyAssigned(
      imageUploadId,
      existingReviewId
    );
  }
}
