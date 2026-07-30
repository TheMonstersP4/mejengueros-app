package io.github.themonstersp4.mejengueros.presentation.profile

import io.github.themonstersp4.mejengueros.domain.model.UserProfile

data class PlayerProfileUiState(
    val userId: String? = null,
    val profile: UserProfile? = null,
    val fallbackDisplayName: String? = null,
    val fallbackEmail: String = "",
    val isImagePickerAvailable: Boolean = false,
    val isUploadingImage: Boolean = false,
    val feedback: PlayerProfileFeedback? = null,
) {
  val displayName: String?
    get() = profile?.name ?: fallbackDisplayName

  val email: String
    get() = profile?.email ?: fallbackEmail

  val pictureUrl: String?
    get() = profile?.pictureUrl

  val provider: String?
    get() = profile?.provider
}

enum class PlayerProfileFeedback {
  ImageUpdated,
  ImageReadFailed,
  ImageUploadFailed,
}
