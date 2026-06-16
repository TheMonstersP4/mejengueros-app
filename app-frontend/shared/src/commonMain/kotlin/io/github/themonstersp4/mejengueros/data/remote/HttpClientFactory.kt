package io.github.themonstersp4.mejengueros.data.remote

import io.ktor.client.HttpClient

expect class HttpClientFactory {
  fun create(): HttpClient
}
