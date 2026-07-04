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
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage

@Composable
actual fun rememberReviewEvidenceImagePicker(
    onImagePicked: (LocalReviewEvidenceImage) -> Unit,
): ReviewEvidenceImagePickerController {
  val context = LocalContext.current
  val callbackRelay = remember { ReviewEvidenceImagePickedCallbackRelay(onImagePicked) }
  SideEffect { callbackRelay.update(onImagePicked) }
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.toLocalReviewEvidenceImage(context.contentResolver)?.let(callbackRelay::dispatch)
      }

  return remember(launcher) {
    ReviewEvidenceImagePickerController(
        isAvailable = true,
        launch = {
          launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
  }
}

internal class ReviewEvidenceImagePickedCallbackRelay(
    initialCallback: (LocalReviewEvidenceImage) -> Unit
) {
  private var callback: (LocalReviewEvidenceImage) -> Unit = initialCallback

  fun update(latestCallback: (LocalReviewEvidenceImage) -> Unit) {
    callback = latestCallback
  }

  fun dispatch(image: LocalReviewEvidenceImage) {
    callback(image)
  }
}

private fun Uri.toLocalReviewEvidenceImage(
    contentResolver: ContentResolver
): LocalReviewEvidenceImage? {
  val bytes = contentResolver.openInputStream(this)?.use { it.readBytes() } ?: return null
  val contentType = contentResolver.getType(this) ?: "image/jpeg"
  val fileName =
      contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
      } ?: "review-evidence-image"

  return LocalReviewEvidenceImage(
      fileName = fileName,
      contentType = contentType,
      bytes = bytes,
      previewUrl = toString(),
  )
}
