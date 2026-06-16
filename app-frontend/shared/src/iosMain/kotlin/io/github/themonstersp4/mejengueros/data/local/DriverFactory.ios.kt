package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
  actual fun createDriver(): SqlDriver = NativeSqliteDriver(AppDatabase.Schema, "mejengueros.db")
}
