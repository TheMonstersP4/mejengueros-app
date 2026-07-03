package io.github.themonstersp4.mejengueros.navigation

import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComplexDetailCourtImagePickerCoordinatorTest {

  @Test
  fun onPickCourtImageStoresRequestedCourtIdAndLaunchesPicker() {
    var launched = false
    var updated = false
    val coordinator =
        ComplexDetailCourtImagePickerCoordinator("complex-id") { _, _, _ -> updated = true }

    coordinator.onPickCourtImage("court-id") { launched = true }

    assertTrue(launched)
    coordinator.onImagePicked(localCourtImage())
    assertTrue(updated)
  }

  @Test
  fun onImagePickedUsesMostRecentlyRequestedCourtId() {
    var receivedComplexId: String? = null
    var receivedCourtId: String? = null
    var receivedImage: LocalCourtImage? = null
    val pickedImage = localCourtImage()
    val coordinator =
        ComplexDetailCourtImagePickerCoordinator("complex-id") { complexId, courtId, image ->
          receivedComplexId = complexId
          receivedCourtId = courtId
          receivedImage = image
        }

    coordinator.onPickCourtImage("court-a") {}
    coordinator.onPickCourtImage("court-b") {}
    coordinator.onImagePicked(pickedImage)

    assertEquals("complex-id", receivedComplexId)
    assertEquals("court-b", receivedCourtId)
    assertEquals(pickedImage, receivedImage)
  }

  @Test
  fun onImagePickedWithoutPendingCourtDoesNothing() {
    var launchedUpdate = false
    val coordinator =
        ComplexDetailCourtImagePickerCoordinator("complex-id") { _, _, _ -> launchedUpdate = true }

    coordinator.onImagePicked(localCourtImage())

    assertFalse(launchedUpdate)
  }

  private fun localCourtImage() =
      LocalCourtImage(
          fileName = "court-a.png",
          contentType = "image/png",
          bytes = byteArrayOf(1, 2, 3),
          previewUrl = "content://court-a.png",
      )
}
