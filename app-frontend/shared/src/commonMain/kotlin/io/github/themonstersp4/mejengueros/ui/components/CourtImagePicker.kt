package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage

data class CourtImagePickerController(
    val isAvailable: Boolean,
    val launch: () -> Unit,
)

@Composable
expect fun rememberCourtImagePicker(
    onImagePicked: (LocalCourtImage) -> Unit,
): CourtImagePickerController
