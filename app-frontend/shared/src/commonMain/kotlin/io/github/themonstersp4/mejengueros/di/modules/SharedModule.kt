package io.github.themonstersp4.mejengueros.di.modules

import org.koin.dsl.module

val sharedModule = module {
  includes(networkModule, dataModule, presentationModule, platformModule())
}
