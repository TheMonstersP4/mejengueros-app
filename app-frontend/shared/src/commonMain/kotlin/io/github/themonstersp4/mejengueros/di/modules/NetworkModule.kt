package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.auth.IAuthTokenProvider
import io.github.themonstersp4.mejengueros.data.remote.AppApiConfig
import io.github.themonstersp4.mejengueros.data.remote.AppApiHttpClientQualifier
import io.github.themonstersp4.mejengueros.data.remote.CognitoJsonContentType
import io.github.themonstersp4.mejengueros.data.remote.HttpClientFactory
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val ConnectTimeoutMillis = 10_000L
private const val RequestTimeoutMillis = 15_000L
private const val SocketTimeoutMillis = 15_000L

val networkModule = module {
  single { Json { ignoreUnknownKeys = true } }
  single {
    val appJson = get<Json>()
    get<HttpClientFactory>().create().config {
      expectSuccess = true
      installSharedClientPlugins(appJson)
    }
  }
  single(named(AppApiHttpClientQualifier)) {
    val appJson = get<Json>()
    val apiConfig = get<AppApiConfig>()
    val tokenProvider = get<IAuthTokenProvider>()
    get<HttpClientFactory>().create().config {
      expectSuccess = true
      installSharedClientPlugins(appJson)
      defaultRequest {
        url(apiConfig.baseUrl)
        tokenProvider.getBearerToken()?.let { token ->
          if (!headers.contains(HttpHeaders.Authorization)) {
            headers.append(HttpHeaders.Authorization, "Bearer $token")
          }
        }
      }
    }
  }
}

private fun HttpClientConfig<*>.installSharedClientPlugins(appJson: Json) {
  install(ContentNegotiation) {
    json(appJson)
    json(appJson, contentType = CognitoJsonContentType)
  }
  install(HttpTimeout) {
    connectTimeoutMillis = ConnectTimeoutMillis
    requestTimeoutMillis = RequestTimeoutMillis
    socketTimeoutMillis = SocketTimeoutMillis
  }
}
