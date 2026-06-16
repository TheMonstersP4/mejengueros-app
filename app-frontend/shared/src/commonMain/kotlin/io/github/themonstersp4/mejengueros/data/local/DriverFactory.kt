package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
  fun createDriver(): SqlDriver
}
