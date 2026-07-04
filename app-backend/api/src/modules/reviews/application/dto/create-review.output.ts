export interface ICreateReviewOutput {
  id: string;
  reservationId: string;
  rating: number;
  comment?: string;
  evidenceImageUploadId?: string;
  createdAt: string;
}
