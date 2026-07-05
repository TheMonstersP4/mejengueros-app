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
import androidx.compose.ui.platform.LocalContext
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage

@Composable
actual fun rememberCourtImagePicker(
    onImagePicked: (LocalCourtImage) -> Unit,
): CourtImagePickerController {
  val context = LocalContext.current
  val callbackRelay = remember { CourtImagePickedCallbackRelay(onImagePicked) }
  SideEffect { callbackRelay.update(onImagePicked) }
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.toLocalCourtImage(context.contentResolver)?.let(callbackRelay::dispatch)
      }

  return remember(launcher) {
    CourtImagePickerController(
        isAvailable = true,
        launch = {
          launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
  }
}

internal class CourtImagePickedCallbackRelay(initialCallback: (LocalCourtImage) -> Unit) {
  private var callback: (LocalCourtImage) -> Unit = initialCallback

  fun update(latestCallback: (LocalCourtImage) -> Unit) {
    callback = latestCallback
  }

  fun dispatch(image: LocalCourtImage) {
    callback(image)
  }
}

private fun Uri.toLocalCourtImage(contentResolver: ContentResolver): LocalCourtImage? {
  val bytes = contentResolver.openInputStream(this)?.use { it.readBytes() } ?: return null
  val contentType = contentResolver.getType(this) ?: "image/jpeg"
  val fileName =
      contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) {
          it.getString(0)
        } else {
          null
        }
      } ?: "court-image"

  return LocalCourtImage(
      fileName = fileName,
      contentType = contentType,
      bytes = bytes,
      previewUrl = toString(),
  )
}
