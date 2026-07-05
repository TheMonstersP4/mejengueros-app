package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.ConfirmedReviewEvidenceImageUpload
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage

interface IReviewEvidenceUploadRemoteDataSource {
  suspend fun uploadReviewEvidence(
      image: LocalReviewEvidenceImage
  ): ConfirmedReviewEvidenceImageUpload
}
