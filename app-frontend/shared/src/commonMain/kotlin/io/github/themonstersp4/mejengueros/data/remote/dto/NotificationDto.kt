package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationsEnvelopeDto(
    val success: Boolean,
    val data: List<NotificationDto> = emptyList(),
)

@Serializable
data class NotificationEnvelopeDto(
    val success: Boolean,
    val data: NotificationDto? = null,
)

@Serializable
data class RealtimeNotificationEnvelopeDto(
    val type: String,
    val data: NotificationDto? = null,
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val status: String,
    val reservationId: String,
    val title: String,
    val message: String,
    val reservation: NotificationReservationDto,
    val createdAt: String,
    val readAt: String? = null,
)

@Serializable
data class NotificationReservationDto(
    val id: String,
    val complexName: String,
    val courtName: String,
    val startsAt: String,
    val endsAt: String,
)
