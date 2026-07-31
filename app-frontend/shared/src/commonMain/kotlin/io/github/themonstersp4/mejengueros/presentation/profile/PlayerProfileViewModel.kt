package io.github.themonstersp4.mejengueros.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.domain.repository.IAuthenticatedUserProfileRepository
import io.github.themonstersp4.mejengueros.domain.repository.IProfileImageRepository
import io.github.themonstersp4.mejengueros.ui.components.ProfileImagePickerResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerProfileViewModel(
    private val authenticatedUserProfileRepository: IAuthenticatedUserProfileRepository,
    private val profileImageRepository: IProfileImageRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(PlayerProfileUiState())
  val uiState: StateFlow<PlayerProfileUiState> = _uiState.asStateFlow()

  init {
    scope.launch {
      authenticatedUserProfileRepository.userProfile.collect { profile ->
        val currentState = _uiState.value
        when {
          profile == null -> _uiState.value = currentState.copy(profile = null)
          profile.id == currentState.userId -> _uiState.value = currentState.copy(profile = profile)
        }
      }
    }
  }

  fun activate(userId: String?, displayName: String?, email: String) {
    val currentState = _uiState.value
    if (currentState.userId == userId) {
      _uiState.value = currentState.copy(fallbackDisplayName = displayName, fallbackEmail = email)
      return
    }

    val cachedProfile =
        authenticatedUserProfileRepository.getUserProfile()?.takeIf { it.id == userId }
    _uiState.value =
        PlayerProfileUiState(
            userId = userId,
            profile = cachedProfile,
            fallbackDisplayName = displayName,
            fallbackEmail = email,
            isImagePickerAvailable = currentState.isImagePickerAvailable,
        )
  }

  fun setImagePickerAvailable(isAvailable: Boolean) {
    _uiState.value = _uiState.value.copy(isImagePickerAvailable = isAvailable)
  }

  fun onPickerResult(result: ProfileImagePickerResult) {
    when (result) {
      is ProfileImagePickerResult.Selected -> upload(result.image)
      is ProfileImagePickerResult.ReadFailed ->
          _uiState.value = _uiState.value.copy(feedback = PlayerProfileFeedback.ImageReadFailed)
      ProfileImagePickerResult.Cancelled,
      ProfileImagePickerResult.Unsupported -> Unit
    }
  }

  fun dismissFeedback() {
    _uiState.value = _uiState.value.copy(feedback = null)
  }

  private fun upload(image: io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage) {
    if (_uiState.value.isUploadingImage) return

    val uploadUserId = _uiState.value.userId
    _uiState.value = _uiState.value.copy(isUploadingImage = true, feedback = null)
    scope.launch {
      try {
        val profile = profileImageRepository.updateProfileImage(image)
        if (_uiState.value.userId == uploadUserId) {
          _uiState.value =
              _uiState.value.copy(
                  userId = profile.id,
                  profile = profile,
                  isUploadingImage = false,
                  feedback = PlayerProfileFeedback.ImageUpdated,
              )
        }
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        if (_uiState.value.userId == uploadUserId) {
          _uiState.value =
              _uiState.value.copy(
                  isUploadingImage = false,
                  feedback = PlayerProfileFeedback.ImageUploadFailed,
              )
        }
      }
    }
  }
}
