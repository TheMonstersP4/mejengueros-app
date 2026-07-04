import { ApiProperty } from '@nestjs/swagger';

export class CreateReviewResponse {
  @ApiProperty({ example: 'review-id' })
  id!: string;

  @ApiProperty({ example: 'reservation-id' })
  reservationId!: string;

  @ApiProperty({ minimum: 1, maximum: 5, example: 1 })
  rating!: number;

  @ApiProperty({ required: false })
  comment?: string;

  @ApiProperty({
    required: false,
    format: 'uuid',
    example: '6f554321-6df0-43c4-b310-f3d7e6bf00a1'
  })
  evidenceImageUploadId?: string;

  @ApiProperty({ example: '2026-07-03T02:00:00.000Z' })
  createdAt!: string;
}
