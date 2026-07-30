package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun rememberProfileImagePicker(
    onResult: (ProfileImagePickerResult) -> Unit,
): ProfileImagePickerController = unsupportedProfileImagePickerController(onResult)
