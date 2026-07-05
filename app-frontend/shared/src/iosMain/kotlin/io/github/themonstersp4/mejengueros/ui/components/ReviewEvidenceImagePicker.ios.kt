package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage

@Composable
actual fun rememberReviewEvidenceImagePicker(
    onImagePicked: (LocalReviewEvidenceImage) -> Unit,
): ReviewEvidenceImagePickerController =
    ReviewEvidenceImagePickerController(isAvailable = false, launch = {})
