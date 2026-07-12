package io.github.themonstersp4.mejengueros.presentation.notifications

import io.github.themonstersp4.mejengueros.domain.model.UserNotification

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<UserNotificationUiModel> = emptyList(),
    val loadErrorMessage: String? = null,
    val realtimeMessage: String? = null,
) {
  val isEmpty: Boolean = !isLoading && loadErrorMessage == null && notifications.isEmpty()
  val unreadCount: Int = notifications.count { !it.isRead }
}

data class UserNotificationUiModel(
    val id: String,
    val title: String,
    val message: String,
    val reservationId: String,
    val complexName: String,
    val courtName: String,
    val startsAt: String,
    val endsAt: String,
    val createdAt: String,
    val isRead: Boolean,
)

fun UserNotification.toUiModel(): UserNotificationUiModel =
    UserNotificationUiModel(
        id = id,
        title = title,
        message = message,
        reservationId = reservation.id,
        complexName = reservation.complexName,
        courtName = reservation.courtName,
        startsAt = reservation.startsAt,
        endsAt = reservation.endsAt,
        createdAt = createdAt,
        isRead = isRead,
    )
