package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.IOAuthBrowser
import io.github.themonstersp4.mejengueros.data.auth.IRandomStringGenerator
import io.github.themonstersp4.mejengueros.data.auth.IosAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.IosOAuthBrowser
import io.github.themonstersp4.mejengueros.data.auth.SecureRandomStringGenerator
import io.github.themonstersp4.mejengueros.data.local.DriverFactory
import io.github.themonstersp4.mejengueros.data.remote.HttpClientFactory
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import io.github.themonstersp4.mejengueros.monitoring.IosSystemErrorReporter
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
  single { DriverFactory() }
  single { HttpClientFactory() }
  single<IAuthSecureStorage> { IosAuthSecureStorage(get()) }
  single<IRandomStringGenerator> { SecureRandomStringGenerator() }
  single<IOAuthBrowser> { IosOAuthBrowser() }
  single<ErrorReporter> { IosSystemErrorReporter() }
}
