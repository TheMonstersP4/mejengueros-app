export interface ICreateReviewInput {
  reservationId: string;
  rating: number;
  comment?: string;
  evidenceImageUploadId?: string;
  questionnaireAnswers?: {
    questionKey: string;
    answerKey: string;
  }[];
}
