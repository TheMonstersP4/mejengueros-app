import {
  Body,
  Controller,
  Get,
  Inject,
  Post,
  UseGuards
} from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
import {
  ApiEnvelopeArrayOk,
  ApiEnvelopeCreated,
  ApiEnvelopeErrors
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import { ConfirmUploadUseCase } from '../../../application/use-cases/confirm-upload.use-case';
import { CreateUploadUrlUseCase } from '../../../application/use-cases/create-upload-url.use-case';
import { ListImageUploadsUseCase } from '../../../application/use-cases/list-image-uploads.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { ConfirmUploadRequest } from '../dto/confirm-upload.request';
import { ConfirmUploadResponse } from '../dto/confirm-upload.response';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { CreateUploadUrlRequest } from '../dto/create-upload-url.request';
import { CreateUploadUrlResponse } from '../dto/create-upload-url.response';
import { ImageUploadResponse } from '../dto/image-upload.response';

/**
 * HTTP endpoints for application-managed file uploads.
 */
@ApiTags('files')
@ApiBearerAuth()
@Controller('files')
export class FilesController {
  constructor(
    @Inject(CreateUploadUrlUseCase)
    private readonly createUploadUrl: CreateUploadUrlUseCase,
    @Inject(ConfirmUploadUseCase)
    private readonly confirmUpload: ConfirmUploadUseCase,
    @Inject(ListImageUploadsUseCase)
    private readonly listImageUploads: ListImageUploadsUseCase
  ) {}

  /**
   * Lists confirmed image uploads.
   *
   * @returns Uploaded image responses.
   */
  @Get('uploads')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List confirmed image uploads.',
    description:
      'Returns confirmed image uploads with short-lived read URLs and uploader snapshots.'
  })
  @ApiEnvelopeArrayOk(
    ImageUploadResponse,
    'Confirmed image uploads wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(401, 502)
  async listUploads(): Promise<ImageUploadResponse[]> {
    return this.listImageUploads.execute();
  }

  /**
   * Creates a presigned URL for direct image upload.
   *
   * @param user - Current authenticated user.
   * @param request - Upload URL request body.
   * @returns Presigned upload URL response.
   */
  @Post('uploads')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Create a presigned image upload URL.',
    description:
      'Validates the requested image purpose, MIME type, and size before returning a short-lived S3 POST form.'
  })
  @ApiBody({
    description: 'Image metadata required to create a direct upload URL.',
    type: CreateUploadUrlRequest
  })
  @ApiEnvelopeCreated(
    CreateUploadUrlResponse,
    'Presigned image upload form wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 413, 415, 502)
  async createUpload(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Body() request: CreateUploadUrlRequest
  ): Promise<CreateUploadUrlResponse> {
    return this.createUploadUrl.execute({
      ownerSub: user.sub,
      purpose: request.purpose,
      contentType: request.contentType,
      sizeBytes: request.sizeBytes
    });
  }

  /**
   * Confirms a direct image upload after the client sends it to S3.
   *
   * @param user - Current authenticated user.
   * @param request - Upload confirmation request body.
   * @returns Confirmed upload response.
   */
  @Post('uploads/confirm')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Confirm a direct S3 image upload.',
    description:
      'Checks the uploaded S3 object, validates ownership, stores image metadata, and returns a short-lived read URL.'
  })
  @ApiBody({
    description: 'Uploaded object information returned by upload URL creation.',
    type: ConfirmUploadRequest
  })
  @ApiEnvelopeCreated(
    ConfirmUploadResponse,
    'Confirmed image upload wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 403, 404, 502)
  async confirm(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Body() request: ConfirmUploadRequest
  ): Promise<ConfirmUploadResponse> {
    return this.confirmUpload.execute({
      ownerSub: user.sub,
      ownerEmail: user.email,
      ownerName: user.name,
      ownerPictureUrl: user.pictureUrl,
      ownerProvider: user.provider,
      purpose: request.purpose,
      objectKey: request.objectKey
    });
  }
}
