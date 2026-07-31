package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface IAuthenticatedUserProfileRepository {
  val userProfile: StateFlow<UserProfile?>

  fun getUserProfile(): UserProfile?

  fun updateUserProfile(profile: UserProfile)
}
