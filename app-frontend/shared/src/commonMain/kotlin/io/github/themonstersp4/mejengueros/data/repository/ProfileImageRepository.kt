package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IProfileImageRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.repository.IAuthenticatedUserProfileRepository
import io.github.themonstersp4.mejengueros.domain.repository.IProfileImageRepository

class ProfileImageRepository(
    private val remoteDataSource: IProfileImageRemoteDataSource,
    private val authenticatedUserProfileRepository: IAuthenticatedUserProfileRepository,
) : IProfileImageRepository {
  override suspend fun updateProfileImage(image: LocalProfileImage): UserProfile {
    val profile = remoteDataSource.updateProfileImage(image)
    authenticatedUserProfileRepository.updateUserProfile(profile)
    return profile
  }
}
