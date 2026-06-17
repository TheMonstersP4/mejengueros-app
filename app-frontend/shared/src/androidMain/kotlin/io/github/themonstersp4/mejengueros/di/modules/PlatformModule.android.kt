package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.auth.AndroidAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.AndroidOAuthBrowser
import io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.IOAuthBrowser
import io.github.themonstersp4.mejengueros.data.auth.IRandomStringGenerator
import io.github.themonstersp4.mejengueros.data.auth.SecureRandomStringGenerator
import io.github.themonstersp4.mejengueros.data.local.DriverFactory
import io.github.themonstersp4.mejengueros.data.remote.HttpClientFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
  single { DriverFactory(androidContext()) }
  single { HttpClientFactory() }
  single<IAuthSecureStorage> { AndroidAuthSecureStorage(androidContext(), get()) }
  single<IRandomStringGenerator> { SecureRandomStringGenerator() }
  single<IOAuthBrowser> { AndroidOAuthBrowser(androidContext()) }
}
