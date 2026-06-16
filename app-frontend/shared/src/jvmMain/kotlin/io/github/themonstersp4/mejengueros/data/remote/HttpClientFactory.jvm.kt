package io.github.themonstersp4.mejengueros.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual class HttpClientFactory {
  actual fun create(): HttpClient = HttpClient(OkHttp)
}
