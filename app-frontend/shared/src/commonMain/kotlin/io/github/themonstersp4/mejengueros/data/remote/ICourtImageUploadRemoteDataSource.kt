package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.ConfirmedCourtImageUpload
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage

interface ICourtImageUploadRemoteDataSource {
  suspend fun uploadCourtImage(image: LocalCourtImage): ConfirmedCourtImageUpload
}
