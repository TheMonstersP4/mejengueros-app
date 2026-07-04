package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IReviewEvidenceUploadRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.ConfirmedReviewEvidenceImageUpload
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage
import io.github.themonstersp4.mejengueros.domain.repository.IReviewEvidenceUploadRepository

class ReviewEvidenceUploadRepository(
    private val remoteDataSource: IReviewEvidenceUploadRemoteDataSource,
) : IReviewEvidenceUploadRepository {
  override suspend fun uploadReviewEvidence(
      image: LocalReviewEvidenceImage
  ): ConfirmedReviewEvidenceImageUpload {
    return remoteDataSource.uploadReviewEvidence(image)
  }
}
