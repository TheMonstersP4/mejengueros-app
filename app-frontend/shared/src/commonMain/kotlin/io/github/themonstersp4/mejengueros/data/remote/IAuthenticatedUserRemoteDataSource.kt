package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.UserProfile

interface IAuthenticatedUserRemoteDataSource {
  suspend fun syncCurrentUser(): UserProfile
}
