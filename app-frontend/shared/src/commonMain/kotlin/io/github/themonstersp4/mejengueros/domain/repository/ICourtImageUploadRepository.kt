package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.ConfirmedCourtImageUpload
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage

interface ICourtImageUploadRepository {
  suspend fun uploadCourtImage(image: LocalCourtImage): ConfirmedCourtImageUpload
}

class NoOpCourtImageUploadRepository : ICourtImageUploadRepository {
  override suspend fun uploadCourtImage(image: LocalCourtImage): ConfirmedCourtImageUpload {
    throw UnsupportedOperationException("Court image uploads are not configured.")
  }
}
