package io.github.themonstersp4.mejengueros.ui.components

import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CourtImagePickedCallbackRelayTest {

  @Test
  fun dispatchUsesLatestUpdatedCallback() {
    val received = mutableListOf<String>()
    val relay = CourtImagePickedCallbackRelay { received += "initial:${it.fileName}" }

    relay.update { received += "updated:${it.fileName}" }
    relay.dispatch(localCourtImage("court-b.png"))

    assertEquals(listOf("updated:court-b.png"), received)
  }

  private fun localCourtImage(fileName: String) =
      LocalCourtImage(
          fileName = fileName,
          contentType = "image/png",
          bytes = byteArrayOf(1, 2, 3),
          previewUrl = "content://$fileName",
      )
}
