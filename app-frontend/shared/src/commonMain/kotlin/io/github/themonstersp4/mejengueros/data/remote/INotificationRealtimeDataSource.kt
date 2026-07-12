package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.UserNotification
import kotlinx.coroutines.flow.Flow

interface INotificationRealtimeDataSource {
  fun observeNotifications(): Flow<UserNotification>
}
