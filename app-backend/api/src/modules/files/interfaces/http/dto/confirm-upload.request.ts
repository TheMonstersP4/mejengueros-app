import { IsIn, IsString, MinLength } from 'class-validator';
import { FilePurpose } from '../../../domain/enums/file-purpose.enum';

/**
 * Request body for confirming a direct image upload.
 */
export class ConfirmUploadRequest {
  /**
   * Business purpose for the uploaded image.
   */
  @IsIn([FilePurpose.ProfileImage])
  purpose!: FilePurpose;

  /**
   * Private S3 object key returned by upload URL creation.
   */
  @IsString()
  @MinLength(1)
  objectKey!: string;
}
