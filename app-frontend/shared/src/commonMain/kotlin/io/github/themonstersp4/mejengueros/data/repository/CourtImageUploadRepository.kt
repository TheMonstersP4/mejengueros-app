package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtImageUploadRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.ConfirmedCourtImageUpload
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.domain.repository.ICourtImageUploadRepository

class CourtImageUploadRepository(
    private val remoteDataSource: ICourtImageUploadRemoteDataSource,
) : ICourtImageUploadRepository {
  override suspend fun uploadCourtImage(image: LocalCourtImage): ConfirmedCourtImageUpload {
    return remoteDataSource.uploadCourtImage(image)
  }
}
