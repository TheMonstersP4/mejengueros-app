package io.github.themonstersp4.mejengueros.monitoring

import io.github.themonstersp4.mejengueros.di.modules.platformModule
import kotlin.test.Test
import kotlin.test.assertFalse
import org.koin.dsl.koinApplication

class AndroidPlatformModuleHostTest {

  @Test
  fun platformModuleProvidesConcreteErrorReporter() {
    val koinApp = koinApplication { modules(platformModule()) }

    try {
      val reporter = koinApp.koin.get<ErrorReporter>()
      assertFalse(reporter is NoOpErrorReporter)
    } finally {
      koinApp.close()
    }
  }
}
