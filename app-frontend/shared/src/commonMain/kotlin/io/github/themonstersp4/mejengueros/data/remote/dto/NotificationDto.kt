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
    val reviewId: String? = null,
    val createdAt: String,
    val readAt: String? = null,
)

@Serializable
data class NotificationReservationDto(
    val id: String,
    // Optional so notifications keep parsing against API versions that predate
    // the court-detail navigation fields (issue #334). Absent -> empty, and the
    // client simply does not offer the court-detail destination.
    val courtId: String = "",
    val complexId: String = "",
    val complexName: String,
    val courtName: String,
    val startsAt: String,
    val endsAt: String,
)
