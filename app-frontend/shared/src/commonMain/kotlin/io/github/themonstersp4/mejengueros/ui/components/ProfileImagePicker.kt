package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable
import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage

enum class ProfileImagePickerAvailability {
  Available,
  Unsupported,
}

sealed interface ProfileImagePickerResult {
  data class Selected(val image: LocalProfileImage) : ProfileImagePickerResult

  data object Cancelled : ProfileImagePickerResult

  data class ReadFailed(val cause: Throwable) : ProfileImagePickerResult

  data object Unsupported : ProfileImagePickerResult
}

data class ProfileImagePickerController(
    val availability: ProfileImagePickerAvailability,
    val launch: () -> Unit,
)

fun unsupportedProfileImagePickerController(
    onResult: (ProfileImagePickerResult) -> Unit
): ProfileImagePickerController =
    ProfileImagePickerController(
        availability = ProfileImagePickerAvailability.Unsupported,
        launch = { onResult(ProfileImagePickerResult.Unsupported) },
    )

@Composable
expect fun rememberProfileImagePicker(
    onResult: (ProfileImagePickerResult) -> Unit,
): ProfileImagePickerController
