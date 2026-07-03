import { ApiProperty } from '@nestjs/swagger';
import { IsUUID } from 'class-validator';

export class UpdateOwnedCourtImageRequest {
  @ApiProperty({
    description: 'Confirmed upload identifier to associate with the owned court image.',
    example: '9f6b4f0f-5f5a-4d8d-8c5e-2b2e7b0f6a3c'
  })
  @IsUUID()
  imageUploadId!: string;
}
