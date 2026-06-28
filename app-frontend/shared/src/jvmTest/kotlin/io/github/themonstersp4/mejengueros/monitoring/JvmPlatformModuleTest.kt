package io.github.themonstersp4.mejengueros.monitoring

import io.github.themonstersp4.mejengueros.di.modules.platformModule
import kotlin.test.Test
import kotlin.test.assertFalse
import org.koin.dsl.koinApplication

class JvmPlatformModuleTest {

  @Test
  fun platformModuleProvidesConcreteErrorReporter() {
    val koinApp = koinApplication { modules(platformModule()) }

    try {
      val reporter = koinApp.koin.get<ErrorReporter>()
      assertFalse(reporter is NoOpErrorReporter)
      reporter.reportRecoverableFailure(
          name = "my_complex_hub_refresh_failed",
          attributes = mapOf("status_code" to "503"),
      )
    } finally {
      koinApp.close()
    }
  }
}
