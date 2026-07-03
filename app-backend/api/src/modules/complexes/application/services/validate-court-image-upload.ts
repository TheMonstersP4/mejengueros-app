import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import type { IComplexRepository } from '../../domain/repositories/complex.repository';
import { InvalidCourtImageUploadError } from '../../domain/errors/invalid-court-image-upload.error';

export async function validateCourtImageUpload(
  complexRepository: Pick<IComplexRepository, 'findCourtIdByImageUploadId'>,
  imageUploadRepository: IImageUploadRepository,
  ownerSub: string,
  imageUploadId?: string,
  currentCourtId?: string
): Promise<void> {
  if (imageUploadId == null) {
    return;
  }

  const imageUpload = await imageUploadRepository.findById(imageUploadId);

  if (imageUpload == null) {
    throw InvalidCourtImageUploadError.notFound(imageUploadId);
  }

  const snapshot = imageUpload.toSnapshot();

  if (snapshot.ownerSub !== ownerSub) {
    throw InvalidCourtImageUploadError.ownerMismatch(
      imageUploadId,
      ownerSub,
      snapshot.ownerSub
    );
  }

  if (snapshot.purpose !== FilePurpose.CourtImage) {
    throw InvalidCourtImageUploadError.invalidPurpose(
      imageUploadId,
      snapshot.purpose
    );
  }

  const existingCourtId = await complexRepository.findCourtIdByImageUploadId(imageUploadId);

  if (existingCourtId != null && existingCourtId !== currentCourtId) {
    throw InvalidCourtImageUploadError.alreadyAssigned(imageUploadId, existingCourtId);
  }
}
