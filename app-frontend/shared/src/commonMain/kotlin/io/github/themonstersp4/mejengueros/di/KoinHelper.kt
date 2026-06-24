package io.github.themonstersp4.mejengueros.di

import io.github.themonstersp4.mejengueros.di.modules.sharedModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes

/**
 * `initKoin()` is invoked from the Android, iOS, and Desktop entrypoints. This guard prevents Koin
 * from being reinitialized when the same runtime touches the shared bridge again, for example when
 * recreating the `UIViewController` on iOS.
 */
private var hasStartedKoin = false

fun initKoin(config: KoinAppDeclaration? = null) {
  if (hasStartedKoin) return

  startKoin {
    includes(config)
    modules(sharedModule)
  }

  hasStartedKoin = true
}
