package io.github.themonstersp4.mejengueros.presentation.notifications

import io.github.themonstersp4.mejengueros.domain.model.UserNotification
import io.github.themonstersp4.mejengueros.domain.model.UserNotificationReservation
import io.github.themonstersp4.mejengueros.domain.model.UserNotificationStatus
import io.github.themonstersp4.mejengueros.domain.model.UserNotificationType
import io.github.themonstersp4.mejengueros.domain.repository.INotificationRepository
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Test
  fun activateLoadsPersistedNotifications() =
      runTest(dispatcher) {
        val repository =
            FakeNotificationRepository(
                initialNotifications = listOf(notification(id = "notification-1"))
            )
        val viewModel =
            NotificationsViewModel(
                notificationRepository = repository,
                coroutineScope = this,
            )
        viewModel.activate("user-1")

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.notifications.size)
        assertEquals("notification-1", viewModel.uiState.value.notifications.first().id)
      }

  @Test
  fun realtimeNotificationIsAddedWhileSessionIsActive() =
      runTest(dispatcher) {
        val realtimeNotifications = MutableSharedFlow<UserNotification>()
        val repository = FakeNotificationRepository(realtimeNotifications = realtimeNotifications)
        val viewModel =
            NotificationsViewModel(
                notificationRepository = repository,
                coroutineScope = this,
            )

        viewModel.activate("user-1")
        runCurrent()
        realtimeNotifications.emit(notification(id = "notification-realtime"))
        advanceUntilIdle()

        assertEquals("notification-realtime", viewModel.uiState.value.notifications.first().id)
        assertEquals(
            "Contanos como estuvo la mejenga",
            viewModel.uiState.value.realtimeMessage,
        )
        coroutineContext.cancelChildren()
      }

  @Test
  fun refreshRetriesAfterTransientLoadFailure() =
      runTest(dispatcher) {
        val repository =
            FakeNotificationRepository(
                initialNotifications = listOf(notification(id = "notification-after-retry")),
                failuresBeforeSuccess = 1,
            )
        val viewModel =
            NotificationsViewModel(
                notificationRepository = repository,
                coroutineScope = this,
            )

        viewModel.activate("user-1")
        runCurrent()

        assertEquals(
            "No pudimos cargar tus notificaciones. Intenta nuevamente.",
            viewModel.uiState.value.loadErrorMessage,
        )

        advanceTimeBy(5_000)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.loadErrorMessage)
        assertEquals("notification-after-retry", viewModel.uiState.value.notifications.first().id)
      }

  @Test
  fun markReadUpdatesTheNotificationInPlace() =
      runTest(dispatcher) {
        val repository =
            FakeNotificationRepository(
                initialNotifications = listOf(notification(id = "notification-1"))
            )
        val viewModel =
            NotificationsViewModel(
                notificationRepository = repository,
                coroutineScope = this,
            )

        viewModel.activate("user-1")
        advanceUntilIdle()
        viewModel.markRead("notification-1")
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.notifications.first().isRead)
      }

  @Test
  fun deactivateClearsNotificationsAndStopsRealtimeCollection() =
      runTest(dispatcher) {
        val realtimeNotifications = MutableSharedFlow<UserNotification>()
        val repository = FakeNotificationRepository(realtimeNotifications = realtimeNotifications)
        val viewModel =
            NotificationsViewModel(
                notificationRepository = repository,
                coroutineScope = this,
            )

        viewModel.activate("user-1")
        runCurrent()
        realtimeNotifications.emit(notification(id = "notification-realtime"))
        advanceUntilIdle()
        viewModel.deactivate()

        assertEquals(0, viewModel.uiState.value.notifications.size)
        assertEquals(0, viewModel.uiState.value.unreadCount)

        realtimeNotifications.tryEmit(notification(id = "notification-after-deactivate"))
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.notifications.size)
      }
}

private class FakeNotificationRepository(
    private val initialNotifications: List<UserNotification> = emptyList(),
    private val realtimeNotifications: Flow<UserNotification> = emptyFlow(),
    private var failuresBeforeSuccess: Int = 0,
) : INotificationRepository {
  var persistedNotifications: List<UserNotification> = initialNotifications

  override suspend fun getNotifications(): List<UserNotification> {
    if (failuresBeforeSuccess > 0) {
      failuresBeforeSuccess -= 1
      error("Temporary notification load failure.")
    }

    return persistedNotifications
  }

  override suspend fun markRead(notificationId: String): UserNotification =
      notification(id = notificationId, status = UserNotificationStatus.Read)

  override fun observeRealtimeNotifications(): Flow<UserNotification> = realtimeNotifications
}

private fun notification(
    id: String,
    status: UserNotificationStatus = UserNotificationStatus.Pending,
): UserNotification =
    UserNotification(
        id = id,
        type = UserNotificationType.ReviewPrompt,
        status = status,
        reservation =
            UserNotificationReservation(
                id = "reservation-1",
                complexName = "Mejengas CR",
                courtName = "Cancha 1",
                startsAt = "2026-07-11T18:00:00.000Z",
                endsAt = "2026-07-11T19:00:00.000Z",
            ),
        title = "Contanos como estuvo la mejenga",
        message = "Tu reserva ya termino.",
        createdAt = "2026-07-11T19:01:00.000Z",
        readAt = if (status == UserNotificationStatus.Read) "2026-07-11T19:05:00.000Z" else null,
    )
