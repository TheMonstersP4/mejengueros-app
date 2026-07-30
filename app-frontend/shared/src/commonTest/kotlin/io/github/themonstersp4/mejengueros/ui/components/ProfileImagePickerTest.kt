package io.github.themonstersp4.mejengueros.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileImagePickerTest {
  @Test
  fun unsupportedControllerReportsUnavailableWithoutLaunchingPlatformApis() {
    var result: ProfileImagePickerResult? = null
    val controller = unsupportedProfileImagePickerController { result = it }

    assertEquals(ProfileImagePickerAvailability.Unsupported, controller.availability)

    controller.launch()

    assertEquals(ProfileImagePickerResult.Unsupported, result)
  }
}
