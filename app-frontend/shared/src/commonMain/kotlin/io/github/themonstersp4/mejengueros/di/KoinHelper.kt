package io.github.themonstersp4.mejengueros.di

import io.github.themonstersp4.mejengueros.di.modules.sharedModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes

private var isKoinInitialized = false

fun initKoin(config: KoinAppDeclaration? = null) {
  if (isKoinInitialized) return

  startKoin {
    includes(config)
    modules(sharedModule)
  }

  isKoinInitialized = true
}
