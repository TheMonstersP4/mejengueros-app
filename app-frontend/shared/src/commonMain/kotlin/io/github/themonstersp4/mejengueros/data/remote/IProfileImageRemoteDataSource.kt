package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage
import io.github.themonstersp4.mejengueros.domain.model.UserProfile

interface IProfileImageRemoteDataSource {
  suspend fun updateProfileImage(image: LocalProfileImage): UserProfile
}
