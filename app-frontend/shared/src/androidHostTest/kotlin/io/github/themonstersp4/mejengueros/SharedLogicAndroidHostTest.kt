package io.github.themonstersp4.mejengueros

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.themonstersp4.mejengueros.data.local.AppDatabase
import io.github.themonstersp4.mejengueros.data.local.AuthLocalDataSource
import io.github.themonstersp4.mejengueros.data.local.DriverFactory
import io.github.themonstersp4.mejengueros.domain.model.AuthSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedLogicAndroidHostTest {

  @Test
  fun androidDriverPersistsAndClearsAuthSession() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val driver = DriverFactory(context).createDriver()
    val database = AppDatabase(driver)
    val dataSource = AuthLocalDataSource(database.authSessionQueries)

    try {
      dataSource.clearSession()

      dataSource.saveSession(sampleSession())
      assertEquals(sampleSession(), dataSource.getSession())

      dataSource.clearSession()
      assertNull(dataSource.getSession())
    } finally {
      driver.close()
    }
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
