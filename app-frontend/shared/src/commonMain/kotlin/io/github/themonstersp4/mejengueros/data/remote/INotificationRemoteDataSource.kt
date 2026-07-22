package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.UserNotification

interface INotificationRemoteDataSource {
  suspend fun getNotifications(): List<UserNotification>

  suspend fun markRead(notificationId: String): UserNotification
}
