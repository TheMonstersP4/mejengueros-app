package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.INotificationRealtimeDataSource
import io.github.themonstersp4.mejengueros.data.remote.INotificationRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.UserNotification
import io.github.themonstersp4.mejengueros.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val remoteDataSource: INotificationRemoteDataSource,
    private val realtimeDataSource: INotificationRealtimeDataSource,
) : INotificationRepository {
  override suspend fun getNotifications(): List<UserNotification> {
    return remoteDataSource.getNotifications()
  }

  override suspend fun markRead(notificationId: String): UserNotification {
    return remoteDataSource.markRead(notificationId)
  }

  override fun observeRealtimeNotifications(): Flow<UserNotification> {
    return realtimeDataSource.observeNotifications()
  }
}
