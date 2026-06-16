package io.github.themonstersp4.mejengueros.di

import io.github.themonstersp4.mejengueros.di.modules.sharedModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes

fun initKoin(config: KoinAppDeclaration? = null) {
  if (GlobalContext.getOrNull() != null) return

  startKoin {
    includes(config)
    modules(sharedModule)
  }
}
