package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.local.DriverFactory
import io.github.themonstersp4.mejengueros.data.remote.HttpClientFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
  single { DriverFactory(androidContext()) }
  single { HttpClientFactory() }
}
