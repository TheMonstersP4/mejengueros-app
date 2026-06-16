package io.github.themonstersp4.mejengueros.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
  actual fun createDriver(): SqlDriver =
      AndroidSqliteDriver(AppDatabase.Schema, context, "mejengueros.db")
}
