package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.auth.IAuthTokenProvider
import io.github.themonstersp4.mejengueros.data.remote.dto.RealtimeNotificationEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.UserNotification
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val WebSocketReconnectDelayMillis = 3_000L
private const val WebSocketKeepAliveIntervalMillis = 240_000L

class NotificationRealtimeDataSource(
    httpClientFactory: HttpClientFactory,
    private val apiConfig: AppApiConfig,
    private val tokenProvider: IAuthTokenProvider,
    private val json: Json,
) : INotificationRealtimeDataSource {
  private val httpClient: HttpClient = httpClientFactory.create().config { install(WebSockets) }

  override fun observeNotifications(): Flow<UserNotification> = flow {
    val token = tokenProvider.getBearerToken() ?: return@flow
    val websocketUrl = apiConfig.websocketUrl.takeIf { it.isNotBlank() } ?: return@flow

    while (currentCoroutineContext().isActive) {
      try {
        httpClient.webSocket(
            urlString = websocketUrl,
            request = { header(HttpHeaders.Authorization, "Bearer $token") },
        ) {
          val keepAliveJob = launch {
            while (isActive) {
              delay(WebSocketKeepAliveIntervalMillis)
              send(Frame.Ping(ByteArray(0)))
            }
          }

          try {
            for (frame in incoming) {
              if (frame is Frame.Text) {
                val envelope =
                    json.decodeFromString<RealtimeNotificationEnvelopeDto>(frame.readText())

                if (envelope.type == "notification.created") {
                  envelope.data?.let { emit(it.toDomain()) }
                }
              }
            }
          } finally {
            keepAliveJob.cancel()
          }
        }
      } catch (error: Throwable) {
        if (error is CancellationException) {
          throw error
        }

        delay(WebSocketReconnectDelayMillis)
      }
    }
  }
}
