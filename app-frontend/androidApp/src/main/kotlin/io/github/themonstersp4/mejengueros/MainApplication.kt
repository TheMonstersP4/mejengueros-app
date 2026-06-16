package io.github.themonstersp4.mejengueros

import android.app.Application
import io.github.themonstersp4.mejengueros.di.initKoin
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    initKoin { androidContext(this@MainApplication) }
  }
}
