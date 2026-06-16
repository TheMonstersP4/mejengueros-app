package io.github.themonstersp4.mejengueros.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.DriverManager
import java.util.Properties

actual class DriverFactory {
  actual fun createDriver(): SqlDriver {
    val databasePath = databasePath()
    Files.createDirectories(databasePath.parent)
    registerSqliteDriver()

    return JdbcSqliteDriver(
        "jdbc:sqlite:${databasePath.toAbsolutePath()}",
        Properties(),
        AppDatabase.Schema,
    )
  }

  private fun registerSqliteDriver() {
    val driverClassName = "org.sqlite.JDBC"
    val hasSqliteDriver =
        DriverManager.getDrivers().asSequence().any { driver ->
          driver::class.java.name == driverClassName
        }

    if (!hasSqliteDriver) {
      Class.forName(driverClassName)
    }
  }

  private fun databasePath(): Path {
    val osName = System.getProperty("os.name")?.lowercase().orEmpty()
    val appDirectory =
        when {
          "mac" in osName ->
              Paths.get(
                  System.getProperty("user.home"),
                  "Library",
                  "Application Support",
                  "io.github.themonstersp4.mejengueros",
              )
          "win" in osName ->
              Paths.get(
                  System.getenv("APPDATA") ?: System.getProperty("user.home"),
                  "io.github.themonstersp4.mejengueros",
              )
          else -> Paths.get(System.getProperty("user.home"), ".local", "share", "mejengueros")
        }

    return appDirectory.resolve("mejengueros.db")
  }
}
