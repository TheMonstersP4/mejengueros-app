package io.github.themonstersp4.mejengueros.domain.model

data class UserNotification(
    val id: String,
    val type: UserNotificationType,
    val status: UserNotificationStatus,
    val reservation: UserNotificationReservation,
    val title: String,
    val message: String,
    val createdAt: String,
    val readAt: String?,
) {
  val isRead: Boolean = status == UserNotificationStatus.Read
}

data class UserNotificationReservation(
    val id: String,
    val complexName: String,
    val courtName: String,
    val startsAt: String,
    val endsAt: String,
)

enum class UserNotificationType {
  ReviewPrompt,
  Unknown,
}

enum class UserNotificationStatus {
  Pending,
  Sent,
  Failed,
  Read,
  Unknown,
}
