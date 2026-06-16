import { IsIn, IsInt, IsMimeType, IsPositive, Max } from 'class-validator';
import { FilePurpose } from '../../../domain/enums/file-purpose.enum';

/**
 * Request body for creating an image upload URL.
 */
export class CreateUploadUrlRequest {
  /**
   * Business purpose for the uploaded image.
   */
  @IsIn([FilePurpose.ProfileImage])
  purpose!: FilePurpose;

  /**
   * Client-declared image MIME type.
   */
  @IsMimeType()
  contentType!: string;

  /**
   * Client-declared file size in bytes.
   */
  @IsInt()
  @IsPositive()
  @Max(8388608)
  sizeBytes!: number;
}
