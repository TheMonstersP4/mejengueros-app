package io.github.themonstersp4.mejengueros.presentation.profile

import io.github.themonstersp4.mejengueros.data.remote.IProfileImageRemoteDataSource
import io.github.themonstersp4.mejengueros.data.repository.ProfileImageRepository
import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.repository.IAuthenticatedUserProfileRepository
import io.github.themonstersp4.mejengueros.domain.repository.IProfileImageRepository
import io.github.themonstersp4.mejengueros.ui.components.ProfileImagePickerResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerProfileViewModelTest {
  @Test
  fun activationWithEmptyCacheRefreshesAndAdoptsSignedPictureUrl() = runTest {
    val refreshed = profile(pictureUrl = "https://signed.example.test/profile.jpg")
    val profileSource =
        FakeAuthenticatedUserProfileRepository(
            initialProfile = null,
            refreshOutcomes = mutableListOf(Result.success(refreshed)),
        )
    val viewModel = PlayerProfileViewModel(profileSource, FakeProfileRepository(), backgroundScope)

    viewModel.activate("user-id", "Fallback name", "fallback@example.com")
    runCurrent()

    assertEquals(1, profileSource.refreshRequests)
    assertEquals(refreshed, viewModel.uiState.value.profile)
    assertEquals("https://signed.example.test/profile.jpg", viewModel.uiState.value.pictureUrl)
    assertFalse(viewModel.uiState.value.isRefreshingProfile)
    assertFalse(viewModel.uiState.value.profileRefreshFailed)
  }

  @Test
  fun dismissingRefreshFailurePreservesProfileIdentityAndImageFeedback() = runTest {
    val profileSource =
        FakeAuthenticatedUserProfileRepository(
            initialProfile = null,
            refreshOutcomes =
                mutableListOf(Result.failure(IllegalStateException("refresh failed"))),
        )
    val viewModel = PlayerProfileViewModel(profileSource, FakeProfileRepository(), backgroundScope)

    viewModel.activate("user-id", "Fallback name", "player@example.com")
    runCurrent()

    assertTrue(viewModel.uiState.value.profileRefreshFailed)
    assertNull(viewModel.uiState.value.profile)
    viewModel.onPickerResult(ProfileImagePickerResult.ReadFailed(IllegalStateException("read")))

    viewModel.dismissProfileRefreshFailure()

    assertEquals(1, profileSource.refreshRequests)
    assertFalse(viewModel.uiState.value.profileRefreshFailed)
    assertEquals("user-id", viewModel.uiState.value.userId)
    assertEquals("Fallback name", viewModel.uiState.value.displayName)
    assertEquals("player@example.com", viewModel.uiState.value.email)
    assertNull(viewModel.uiState.value.profile)
    assertEquals(PlayerProfileFeedback.ImageReadFailed, viewModel.uiState.value.feedback)
  }

  @Test
  fun successfulProfileWithoutPictureDoesNotRefreshAgain() = runTest {
    val profileSource =
        FakeAuthenticatedUserProfileRepository(
            initialProfile = null,
            refreshOutcomes = mutableListOf(Result.success(profile(pictureUrl = null))),
        )
    val viewModel = PlayerProfileViewModel(profileSource, FakeProfileRepository(), backgroundScope)

    viewModel.activate("user-id", null, "player@example.com")
    runCurrent()
    viewModel.activate("user-id", null, "player@example.com")
    runCurrent()

    assertEquals(1, profileSource.refreshRequests)
    assertNull(viewModel.uiState.value.pictureUrl)
    assertFalse(viewModel.uiState.value.profileRefreshFailed)
  }

  @Test
  fun existingMatchingProfileAvoidsRefresh() = runTest {
    val profileSource = FakeAuthenticatedUserProfileRepository(profile())
    val viewModel = PlayerProfileViewModel(profileSource, FakeProfileRepository(), backgroundScope)

    viewModel.activate("user-id", null, "player@example.com")
    runCurrent()

    assertEquals(0, profileSource.refreshRequests)
    assertEquals(profile(), viewModel.uiState.value.profile)
  }

  @Test
  fun cancelledRefreshIsNotConvertedToFailure() = runTest {
    val profileSource =
        FakeAuthenticatedUserProfileRepository(
            initialProfile = null,
            refreshOutcomes =
                mutableListOf(Result.failure(CancellationException("refresh cancelled"))),
        )
    val viewModel = PlayerProfileViewModel(profileSource, FakeProfileRepository(), backgroundScope)

    viewModel.activate("user-id", null, "player@example.com")
    runCurrent()

    assertEquals(1, profileSource.refreshRequests)
    assertFalse(viewModel.uiState.value.isRefreshingProfile)
    assertFalse(viewModel.uiState.value.profileRefreshFailed)
  }

  @Test
  fun activateUsesCachedProfileAndFallsBackToAuthenticatedIdentity() = runTest {
    val cached = profile(pictureUrl = "https://example.test/original.jpg")
    val viewModel =
        PlayerProfileViewModel(
            FakeAuthenticatedUserProfileRepository(cached),
            FakeProfileRepository(),
            backgroundScope,
        )

    viewModel.activate("user-id", "Fallback name", "fallback@example.com")

    assertEquals("Player One", viewModel.uiState.value.displayName)
    assertEquals("player@example.com", viewModel.uiState.value.email)
    assertEquals("https://example.test/original.jpg", viewModel.uiState.value.pictureUrl)
    assertEquals("Google", viewModel.uiState.value.provider)

    viewModel.activate("other-user", "Other Player", "other@example.com")

    assertEquals("Other Player", viewModel.uiState.value.displayName)
    assertEquals("other@example.com", viewModel.uiState.value.email)
    assertNull(viewModel.uiState.value.pictureUrl)
  }

  @Test
  fun activeUserReceivesRefreshedPictureWithoutReactivation() = runTest {
    val profileSource =
        FakeAuthenticatedUserProfileRepository(
            profile(pictureUrl = "https://example.test/original.jpg")
        )
    val viewModel = PlayerProfileViewModel(profileSource, FakeProfileRepository(), backgroundScope)
    viewModel.activate("user-id", null, "")

    profileSource.updateUserProfile(profile(pictureUrl = "https://example.test/refreshed.jpg"))
    runCurrent()

    assertEquals("https://example.test/refreshed.jpg", viewModel.uiState.value.pictureUrl)
  }

  @Test
  fun clearingAuthenticatedProfileClearsActiveScreenProfile() = runTest {
    val profileSource =
        FakeAuthenticatedUserProfileRepository(
            profile(pictureUrl = "https://example.test/original.jpg")
        )
    val viewModel = PlayerProfileViewModel(profileSource, FakeProfileRepository(), backgroundScope)
    viewModel.activate("user-id", null, "")

    profileSource.clear()
    runCurrent()

    assertNull(viewModel.uiState.value.profile)
  }

  @Test
  fun anotherUsersEmissionDoesNotOverwriteActiveScreen() = runTest {
    val activeProfile = profile(pictureUrl = "https://example.test/active.jpg")
    val profileSource = FakeAuthenticatedUserProfileRepository(activeProfile)
    val viewModel = PlayerProfileViewModel(profileSource, FakeProfileRepository(), backgroundScope)
    viewModel.activate("user-id", null, "")

    profileSource.updateUserProfile(
        activeProfile.copy(id = "other-user", pictureUrl = "https://example.test/other.jpg")
    )
    runCurrent()

    assertEquals(activeProfile, viewModel.uiState.value.profile)
  }

  @Test
  fun uploadPreservesCurrentImageBlocksDuplicatesAndAppliesAssociatedProfile() = runTest {
    val uploadResult = CompletableDeferred<UserProfile>()
    val repository = FakeProfileRepository(result = uploadResult)
    val viewModel =
        PlayerProfileViewModel(
            FakeAuthenticatedUserProfileRepository(
                profile(pictureUrl = "https://example.test/original.jpg")
            ),
            repository,
            backgroundScope,
        )
    viewModel.activate("user-id", null, "")

    viewModel.onPickerResult(ProfileImagePickerResult.Selected(localImage()))
    viewModel.onPickerResult(ProfileImagePickerResult.Selected(localImage()))
    runCurrent()

    assertTrue(viewModel.uiState.value.isUploadingImage)
    assertEquals("https://example.test/original.jpg", viewModel.uiState.value.pictureUrl)
    assertEquals(1, repository.requests)

    uploadResult.complete(profile(pictureUrl = "https://example.test/updated.jpg"))
    runCurrent()

    assertFalse(viewModel.uiState.value.isUploadingImage)
    assertEquals("https://example.test/updated.jpg", viewModel.uiState.value.pictureUrl)
    assertEquals(PlayerProfileFeedback.ImageUpdated, viewModel.uiState.value.feedback)
  }

  @Test
  fun failedUploadPreservesImageAndExposesFeedback() = runTest {
    val repository = FakeProfileRepository(failure = IllegalStateException("upload failed"))
    val viewModel =
        PlayerProfileViewModel(
            FakeAuthenticatedUserProfileRepository(
                profile(pictureUrl = "https://example.test/original.jpg")
            ),
            repository,
            backgroundScope,
        )
    viewModel.activate("user-id", null, "")

    viewModel.onPickerResult(ProfileImagePickerResult.Selected(localImage()))
    runCurrent()

    assertFalse(viewModel.uiState.value.isUploadingImage)
    assertEquals("https://example.test/original.jpg", viewModel.uiState.value.pictureUrl)
    assertEquals(PlayerProfileFeedback.ImageUploadFailed, viewModel.uiState.value.feedback)
  }

  @Test
  fun cancellationAndUnsupportedResultsAreNoOpsWhileReadFailureIsVisible() = runTest {
    val repository = FakeProfileRepository()
    val viewModel =
        PlayerProfileViewModel(
            FakeAuthenticatedUserProfileRepository(profile()),
            repository,
            backgroundScope,
        )
    viewModel.activate("user-id", null, "")

    viewModel.onPickerResult(ProfileImagePickerResult.Cancelled)
    viewModel.onPickerResult(ProfileImagePickerResult.Unsupported)

    assertNull(viewModel.uiState.value.feedback)
    assertEquals(0, repository.requests)

    viewModel.onPickerResult(ProfileImagePickerResult.ReadFailed(IllegalStateException("read")))

    assertEquals(PlayerProfileFeedback.ImageReadFailed, viewModel.uiState.value.feedback)
    assertEquals(0, repository.requests)
  }

  @Test
  fun dismissFeedbackClearsCurrentFeedback() = runTest {
    val viewModel =
        PlayerProfileViewModel(
            FakeAuthenticatedUserProfileRepository(profile()),
            FakeProfileRepository(),
            backgroundScope,
        )
    viewModel.activate("user-id", null, "")
    viewModel.onPickerResult(ProfileImagePickerResult.ReadFailed(IllegalStateException("read")))

    viewModel.dismissFeedback()

    assertNull(viewModel.uiState.value.feedback)
  }

  @Test
  fun completedUploadCannotOverwriteAReplacementAuthenticatedUser() = runTest {
    val uploadResult = CompletableDeferred<UserProfile>()
    val viewModel =
        PlayerProfileViewModel(
            FakeAuthenticatedUserProfileRepository(profile()),
            FakeProfileRepository(result = uploadResult),
            backgroundScope,
        )
    viewModel.activate("user-id", null, "")
    viewModel.onPickerResult(ProfileImagePickerResult.Selected(localImage()))
    runCurrent()

    viewModel.activate("replacement-id", "Replacement", "replacement@example.com")
    uploadResult.complete(profile(pictureUrl = "https://example.test/stale.jpg"))
    runCurrent()

    assertEquals("replacement-id", viewModel.uiState.value.userId)
    assertEquals("Replacement", viewModel.uiState.value.displayName)
    assertNull(viewModel.uiState.value.pictureUrl)
    assertNull(viewModel.uiState.value.feedback)
  }

  @Test
  fun recreatedViewModelReadsPictureUpdatedBySuccessfulAssociation() = runTest {
    val profileSource =
        FakeAuthenticatedUserProfileRepository(
            profile(pictureUrl = "https://example.test/original.jpg")
        )
    val updatedProfile = profile(pictureUrl = "https://example.test/updated.jpg")
    val profileImageRepository =
        ProfileImageRepository(
            remoteDataSource = FakeProfileImageRemoteDataSource(updatedProfile),
            authenticatedUserProfileRepository = profileSource,
        )
    val initialViewModel =
        PlayerProfileViewModel(profileSource, profileImageRepository, backgroundScope)
    initialViewModel.activate("user-id", null, "")

    initialViewModel.onPickerResult(ProfileImagePickerResult.Selected(localImage()))
    runCurrent()

    val recreatedViewModel =
        PlayerProfileViewModel(profileSource, profileImageRepository, backgroundScope)
    recreatedViewModel.activate("user-id", null, "")

    assertEquals("https://example.test/updated.jpg", recreatedViewModel.uiState.value.pictureUrl)
  }

  private fun profile(pictureUrl: String? = null) =
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
          previewUrl = "content://profile.png",
      )

  private class FakeProfileRepository(
      private val result: CompletableDeferred<UserProfile>? = null,
      private val failure: Throwable? = null,
  ) : IProfileImageRepository {
    var requests = 0

    override suspend fun updateProfileImage(image: LocalProfileImage): UserProfile {
      requests += 1
      failure?.let { throw it }
      return result?.await()
          ?: UserProfile("user-id", emptyList(), pictureUrl = "https://example.test/profile.jpg")
    }
  }

  private class FakeAuthenticatedUserProfileRepository(
      initialProfile: UserProfile?,
      private val refreshOutcomes: MutableList<Result<UserProfile>> = mutableListOf(),
  ) : IAuthenticatedUserProfileRepository {
    private val _userProfile = MutableStateFlow(initialProfile)
    override val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    var refreshRequests = 0

    override fun getUserProfile(): UserProfile? = _userProfile.value

    override suspend fun refreshAuthenticatedUserProfile(): UserProfile {
      refreshRequests += 1
      val profile =
          refreshOutcomes.removeFirstOrNull()?.getOrThrow() ?: checkNotNull(_userProfile.value)
      _userProfile.value = profile
      return profile
    }

    override fun updateUserProfile(profile: UserProfile) {
      _userProfile.value = profile
    }

    fun clear() {
      _userProfile.value = null
    }
  }

  private class FakeProfileImageRemoteDataSource(
      private val profile: UserProfile,
  ) : IProfileImageRemoteDataSource {
    override suspend fun updateProfileImage(image: LocalProfileImage): UserProfile = profile
  }
}
