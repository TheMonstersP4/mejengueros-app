package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage

data class ReviewEvidenceImagePickerController(
    val isAvailable: Boolean,
    val launch: () -> Unit,
)

@Composable
expect fun rememberReviewEvidenceImagePicker(
    onImagePicked: (LocalReviewEvidenceImage) -> Unit,
): ReviewEvidenceImagePickerController
