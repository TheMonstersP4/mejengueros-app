package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.UserProfile

interface IAuthenticatedUserProfileRepository {
  fun getUserProfile(): UserProfile?

  fun updateUserProfile(profile: UserProfile)
}
