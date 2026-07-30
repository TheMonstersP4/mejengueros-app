package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IProfileImageRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.repository.IAuthenticatedUserProfileRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class ProfileImageRepositoryTest {
  @Test
  fun successfulAssociationUpdatesAuthenticatedProfileSource() = runTest {
    val original = profile("https://example.test/original.jpg")
    val updated = profile("https://example.test/updated.jpg")
    val profileSource = FakeAuthenticatedUserProfileRepository(original)
    val repository = ProfileImageRepository(FakeRemoteDataSource(result = updated), profileSource)

    val result = repository.updateProfileImage(localImage())

    assertEquals(updated, result)
    assertEquals(updated, profileSource.getUserProfile())
  }

  @Test
  fun failedAssociationLeavesAuthenticatedProfileSourceIntact() = runTest {
    val original = profile("https://example.test/original.jpg")
    val profileSource = FakeAuthenticatedUserProfileRepository(original)
    val repository =
        ProfileImageRepository(
            FakeRemoteDataSource(failure = IllegalStateException("association failed")),
            profileSource,
        )

    assertFailsWith<IllegalStateException> { repository.updateProfileImage(localImage()) }

    assertEquals(original, profileSource.getUserProfile())
  }

  @Test
  fun cancelledAssociationPropagatesAndLeavesAuthenticatedProfileSourceIntact() = runTest {
    val original = profile("https://example.test/original.jpg")
    val profileSource = FakeAuthenticatedUserProfileRepository(original)
    val repository =
        ProfileImageRepository(
            FakeRemoteDataSource(failure = CancellationException("cancelled")),
            profileSource,
        )

    assertFailsWith<CancellationException> { repository.updateProfileImage(localImage()) }

    assertEquals(original, profileSource.getUserProfile())
  }

  private fun profile(pictureUrl: String) =
      UserProfile(
          id = "user-id",
          roles = emptyList(),
          email = "player@example.com",
          name = "Player One",
          pictureUrl = pictureUrl,
          provider = "Google",
      )

  private fun localImage() =
      LocalProfileImage(
          fileName = "profile.png",
          contentType = "image/png",
          bytes = byteArrayOf(1, 2, 3),
      )

  private class FakeRemoteDataSource(
      private val result: UserProfile? = null,
      private val failure: Throwable? = null,
  ) : IProfileImageRemoteDataSource {
    override suspend fun updateProfileImage(image: LocalProfileImage): UserProfile {
      failure?.let { throw it }
      return checkNotNull(result)
    }
  }

  private class FakeAuthenticatedUserProfileRepository(initialProfile: UserProfile?) :
      IAuthenticatedUserProfileRepository {
    private var profile = initialProfile

    override fun getUserProfile(): UserProfile? = profile

    override fun updateUserProfile(profile: UserProfile) {
      this.profile = profile
    }
  }
}
