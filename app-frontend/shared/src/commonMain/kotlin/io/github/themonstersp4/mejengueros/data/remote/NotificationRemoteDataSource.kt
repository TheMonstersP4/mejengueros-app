package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.NotificationDto
import io.github.themonstersp4.mejengueros.data.remote.dto.NotificationEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.NotificationsEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.UserNotification
import io.github.themonstersp4.mejengueros.domain.model.UserNotificationReservation
import io.github.themonstersp4.mejengueros.domain.model.UserNotificationStatus
import io.github.themonstersp4.mejengueros.domain.model.UserNotificationType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.patch
import kotlinx.serialization.json.Json

class NotificationRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : INotificationRemoteDataSource {
  override suspend fun getNotifications(): List<UserNotification> {
    return try {
      httpClient
          .get("/v1/notifications")
          .body<NotificationsEnvelopeDto>()
          .data
          .map(NotificationDto::toDomain)
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun markRead(notificationId: String): UserNotification {
    return try {
      httpClient
          .patch("/v1/notifications/$notificationId/read")
          .body<NotificationEnvelopeDto>()
          .data
          ?.toDomain() ?: error("Notification response did not include data.")
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}

fun NotificationDto.toDomain(): UserNotification =
    UserNotification(
        id = id,
        type = type.toNotificationType(),
        status = status.toNotificationStatus(),
        reservation =
            UserNotificationReservation(
                id = reservation.id,
                complexName = reservation.complexName,
                courtName = reservation.courtName,
                startsAt = reservation.startsAt,
                endsAt = reservation.endsAt,
            ),
        title = title,
        message = message,
        createdAt = createdAt,
        readAt = readAt,
    )

private fun String.toNotificationType(): UserNotificationType =
    when (this) {
      "REVIEW_PROMPT" -> UserNotificationType.ReviewPrompt
      else -> UserNotificationType.Unknown
    }

private fun String.toNotificationStatus(): UserNotificationStatus =
    when (this) {
      "PENDING" -> UserNotificationStatus.Pending
      "SENT" -> UserNotificationStatus.Sent
      "FAILED" -> UserNotificationStatus.Failed
      "READ" -> UserNotificationStatus.Read
      else -> UserNotificationStatus.Unknown
    }
