package io.github.themonstersp4.mejengueros.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileImagePickerHostTest {
  @Test
  fun callbackRelayKeepsLatestPickerResultConsumer() {
    val received = mutableListOf<ProfileImagePickerResult>()
    val relay = ProfileImagePickerCallbackRelay { received += ProfileImagePickerResult.Unsupported }
    relay.update { received += it }

    relay.dispatch(ProfileImagePickerResult.Cancelled)
    relay.dispatch(ProfileImagePickerResult.ReadFailed(IllegalStateException("read failed")))

    assertEquals(ProfileImagePickerResult.Cancelled, received.first())
    assertEquals(true, received.last() is ProfileImagePickerResult.ReadFailed)
  }
}
