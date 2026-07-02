package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage

@Composable
actual fun rememberCourtImagePicker(
    onImagePicked: (LocalCourtImage) -> Unit,
): CourtImagePickerController = CourtImagePickerController(isAvailable = false, launch = {})
