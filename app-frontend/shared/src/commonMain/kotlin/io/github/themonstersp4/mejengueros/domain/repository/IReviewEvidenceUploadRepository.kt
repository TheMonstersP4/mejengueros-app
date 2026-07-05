package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.ConfirmedReviewEvidenceImageUpload
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage

interface IReviewEvidenceUploadRepository {
  suspend fun uploadReviewEvidence(
      image: LocalReviewEvidenceImage
  ): ConfirmedReviewEvidenceImageUpload
}

class NoOpReviewEvidenceUploadRepository : IReviewEvidenceUploadRepository {
  override suspend fun uploadReviewEvidence(
      image: LocalReviewEvidenceImage
  ): ConfirmedReviewEvidenceImageUpload {
    throw UnsupportedOperationException("Review evidence uploads are not configured.")
  }
}
