package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage
import io.github.themonstersp4.mejengueros.domain.model.UserProfile

interface IProfileImageRepository {
  suspend fun updateProfileImage(image: LocalProfileImage): UserProfile
}
