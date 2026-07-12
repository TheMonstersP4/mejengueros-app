package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.UserNotification
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {
  suspend fun getNotifications(): List<UserNotification>

  suspend fun markRead(notificationId: String): UserNotification

  fun observeRealtimeNotifications(): Flow<UserNotification>
}
