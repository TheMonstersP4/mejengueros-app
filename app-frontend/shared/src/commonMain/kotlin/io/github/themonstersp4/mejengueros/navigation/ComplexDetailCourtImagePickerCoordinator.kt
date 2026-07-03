package io.github.themonstersp4.mejengueros.navigation

import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage

internal class ComplexDetailCourtImagePickerCoordinator(
    private val complexId: String,
    private val updateCourtImage: (String, String, LocalCourtImage) -> Unit,
) {
  private var pickedCourtId: String? = null

  fun onPickCourtImage(courtId: String, launchPicker: () -> Unit) {
    pickedCourtId = courtId
    launchPicker()
  }

  fun onImagePicked(image: LocalCourtImage) {
    pickedCourtId?.let { courtId -> updateCourtImage(complexId, courtId, image) }
  }
}
