package io.github.themonstersp4.mejengueros.ui.components

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage

@Composable
actual fun rememberCourtImagePicker(
    onImagePicked: (LocalCourtImage) -> Unit,
): CourtImagePickerController {
  val context = LocalContext.current
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.toLocalCourtImage(context.contentResolver)?.let(onImagePicked)
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
