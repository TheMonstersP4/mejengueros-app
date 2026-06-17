package io.github.themonstersp4.mejengueros

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.themonstersp4.mejengueros.data.auth.AndroidAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.local.PendingOAuthState
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedLogicAndroidHostTest {

  @Test
  fun androidSecureStoragePersistsAndClearsAuthSession() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = AndroidAuthSecureStorage(testPreferences(context), Json)

    storage.clearSession()

    storage.saveSession(sampleSession())
    assertEquals(sampleSession(), storage.getSession())

    storage.clearSession()
    assertNull(storage.getSession())
  }

  @Test
  fun androidSecureStoragePersistsAndClearsOAuthState() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = AndroidAuthSecureStorage(testPreferences(context), Json)
    val state = PendingOAuthState(state = "state", codeVerifier = "verifier")

    storage.clearOAuthState()

    storage.saveOAuthState(state)
    assertEquals(state, storage.getOAuthState())

    storage.clearOAuthState()
    assertNull(storage.getOAuthState())
  }

  private fun testPreferences(context: Context) =
      context.getSharedPreferences("auth-secure-storage-test", Context.MODE_PRIVATE).also {
        it.edit().clear().commit()
      }

  private fun sampleSession(): AuthSession =
      AuthSession(
          sub = "misty",
          email = "misty@example.com",
          displayName = "Misty",
          provider = "Google",
          idToken = "id-token",
          accessToken = "access-token",
          refreshToken = "refresh-token",
          expiresAtEpochSeconds = 4102444800,
      )
}
