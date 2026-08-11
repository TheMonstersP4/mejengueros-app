import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  ArrayMinSize,
  IsArray,
  IsInt,
  IsOptional,
  IsString,
  IsUUID,
  Max,
  Min,
  MinLength,
  ValidateNested
} from 'class-validator';
import {
  REVIEW_QUESTIONNAIRE_REQUIRED_COUNT,
  REVIEW_QUESTIONNAIRE_REQUIRED_QUESTION_KEYS
} from '../../../domain/review-questionnaire.catalog';

export class ReviewQuestionnaireAnswerRequest {
  @ApiProperty({
    enum: REVIEW_QUESTIONNAIRE_REQUIRED_QUESTION_KEYS,
    example: 'FIELD_CONDITION'
  })
  @IsString()
  questionKey!: string;

  @ApiProperty({ example: 'GOOD' })
  @IsString()
  answerKey!: string;
}

export class CreateReviewRequest {
  @ApiProperty({ format: 'uuid' })
  @IsUUID()
  reservationId!: string;

  @ApiProperty({ minimum: 1, maximum: 5, example: 1 })
  @IsInt()
  @Min(1)
  @Max(5)
  rating!: number;

  @ApiProperty({
    required: false,
    example: 'La iluminación falló toda la hora y la cancha estaba muy resbalosa.'
  })
  @IsOptional()
  @IsString()
  comment?: string;

  @ApiProperty({ required: false, format: 'uuid' })
  @IsOptional()
  @IsUUID()
  @MinLength(1)
  evidenceImageUploadId?: string;

  @ApiProperty({
    type: [ReviewQuestionnaireAnswerRequest],
    minItems: REVIEW_QUESTIONNAIRE_REQUIRED_COUNT,
    example: [
      { questionKey: 'FIELD_CONDITION', answerKey: 'GOOD' },
      { questionKey: 'LIGHTING', answerKey: 'GOOD' },
      { questionKey: 'WOULD_RETURN', answerKey: 'YES' }
    ]
  })
  @IsArray()
  @ArrayMinSize(REVIEW_QUESTIONNAIRE_REQUIRED_COUNT)
  @ValidateNested({ each: true })
  @Type(() => ReviewQuestionnaireAnswerRequest)
  questionnaireAnswers!: ReviewQuestionnaireAnswerRequest[];
}
