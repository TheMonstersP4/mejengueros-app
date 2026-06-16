package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.remote.HttpClientFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

private const val ConnectTimeoutMillis = 10_000L
private const val RequestTimeoutMillis = 15_000L
private const val SocketTimeoutMillis = 15_000L

val networkModule = module {
  single {
    get<HttpClientFactory>().create().config {
      expectSuccess = true
      install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      install(HttpTimeout) {
        connectTimeoutMillis = ConnectTimeoutMillis
        requestTimeoutMillis = RequestTimeoutMillis
        socketTimeoutMillis = SocketTimeoutMillis
      }
    }
  }
}
