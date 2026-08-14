export interface ICreateReviewOutput {
  id: string;
  reservationId: string;
  rating: number;
  comment?: string;
  evidenceImageUploadId?: string;
  questionnaireAnswers: {
    questionKey: string;
    answerKey: string;
  }[];
  createdAt: string;
}
