package io.github.themonstersp4.mejengueros.ui.components

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberProfileImagePicker(
    onResult: (ProfileImagePickerResult) -> Unit,
): ProfileImagePickerController {
  val contentResolver = LocalContext.current.contentResolver
  val coroutineScope = rememberCoroutineScope()
  val callbackRelay = remember { ProfileImagePickerCallbackRelay(onResult) }
  SideEffect { callbackRelay.update(onResult) }
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
          callbackRelay.dispatch(ProfileImagePickerResult.Cancelled)
        } else {
          coroutineScope.launch {
            callbackRelay.dispatch(uri.toProfileImagePickerResult(contentResolver))
          }
        }
      }

  return remember(launcher) {
    ProfileImagePickerController(
        availability = ProfileImagePickerAvailability.Available,
        launch = {
          launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
  }
}

internal class ProfileImagePickerCallbackRelay(
    initialCallback: (ProfileImagePickerResult) -> Unit
) {
  private var callback: (ProfileImagePickerResult) -> Unit = initialCallback

  fun update(latestCallback: (ProfileImagePickerResult) -> Unit) {
    callback = latestCallback
  }

  fun dispatch(result: ProfileImagePickerResult) {
    callback(result)
  }
}

private suspend fun Uri.toProfileImagePickerResult(
    contentResolver: ContentResolver
): ProfileImagePickerResult =
    try {
      withContext(Dispatchers.IO) {
        val bytes =
            contentResolver.openInputStream(this@toProfileImagePickerResult)?.use { it.readBytes() }
                ?: error("The selected profile image content could not be opened.")
        val contentType = contentResolver.getType(this@toProfileImagePickerResult) ?: "image/jpeg"
        val fileName =
            contentResolver
                .query(
                    this@toProfileImagePickerResult,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )
                ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "profile-image"

        ProfileImagePickerResult.Selected(
            LocalProfileImage(
                fileName = fileName,
                contentType = contentType,
                bytes = bytes,
                previewUrl = toString(),
            )
        )
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      ProfileImagePickerResult.ReadFailed(error)
    }
