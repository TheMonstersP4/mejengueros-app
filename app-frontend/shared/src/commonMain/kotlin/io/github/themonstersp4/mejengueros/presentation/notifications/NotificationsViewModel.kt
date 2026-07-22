package io.github.themonstersp4.mejengueros.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.UserNotification
import io.github.themonstersp4.mejengueros.domain.repository.INotificationRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import io.github.themonstersp4.mejengueros.monitoring.NoOpErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val NotificationLoadRetryDelayMillis = 5_000L

class NotificationsViewModel(
    private val notificationRepository: INotificationRepository,
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val scope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(NotificationsUiState())
  private var refreshJob: Job? = null
  private var realtimeJob: Job? = null
  private var retryRefreshJob: Job? = null
  private var activeSessionKey: String? = null
  val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

  fun activate(sessionKey: String) {
    if (activeSessionKey == sessionKey && realtimeJob != null) {
      return
    }

    deactivate()
    activeSessionKey = sessionKey
    refresh()
    startRealtime()
  }

  fun deactivate() {
    refreshJob?.cancel()
    realtimeJob?.cancel()
    retryRefreshJob?.cancel()
    refreshJob = null
    realtimeJob = null
    retryRefreshJob = null
    activeSessionKey = null
    _uiState.value = NotificationsUiState()
  }

  fun refresh() {
    if (activeSessionKey == null) {
      return
    }

    retryRefreshJob?.cancel()
    retryRefreshJob = null
    startRefresh()
  }

  private fun startRefresh() {
    refreshJob?.cancel()
    _uiState.value = _uiState.value.copy(isLoading = true, loadErrorMessage = null)

    refreshJob =
        scope.launch {
          runCatching { notificationRepository.getNotifications() }
              .onSuccess { notifications ->
                val loadedNotifications = notifications.map(UserNotification::toUiModel)
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        notifications = mergeLoadedNotifications(loadedNotifications),
                        loadErrorMessage = null,
                    )
                retryRefreshJob?.cancel()
                retryRefreshJob = null
              }
              .onFailure { error ->
                if (error is CancellationException) return@onFailure
                errorReporter.reportRecoverableFailure(
                    name = "notifications_load_failed",
                    attributes = error.toReportAttributes(),
                )
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        loadErrorMessage = error.toUserMessage(),
                    )
                scheduleRefreshRetry()
              }
        }
  }

  fun markRead(notificationId: String) {
    scope.launch {
      runCatching { notificationRepository.markRead(notificationId) }
          .onSuccess { updatedNotification -> upsertNotification(updatedNotification) }
          .onFailure { error ->
            if (error is CancellationException) return@onFailure
            errorReporter.reportRecoverableFailure(
                name = "notification_mark_read_failed",
                attributes = error.toReportAttributes(),
            )
          }
    }
  }

  private fun startRealtime() {
    if (realtimeJob != null) {
      return
    }

    realtimeJob =
        scope.launch {
          runCatching {
                notificationRepository.observeRealtimeNotifications().collect { notification ->
                  upsertNotification(notification, realtimeMessage = notification.title)
                }
              }
              .onFailure { error ->
                if (error is CancellationException) return@onFailure
                errorReporter.reportRecoverableFailure(
                    name = "notifications_realtime_failed",
                    attributes = error.toReportAttributes(),
                )
              }
        }
  }

  private fun scheduleRefreshRetry() {
    val sessionKey = activeSessionKey ?: return

    retryRefreshJob?.cancel()
    retryRefreshJob =
        scope.launch {
          delay(NotificationLoadRetryDelayMillis)
          if (activeSessionKey == sessionKey) {
            startRefresh()
          }
        }
  }

  private fun upsertNotification(
      notification: UserNotification,
      realtimeMessage: String? = _uiState.value.realtimeMessage,
  ) {
    val item = notification.toUiModel()
    val current = _uiState.value.notifications.filterNot { it.id == item.id }

    _uiState.value =
        _uiState.value.copy(
            notifications = listOf(item) + current,
            realtimeMessage = realtimeMessage,
        )
  }

  private fun mergeLoadedNotifications(
      loadedNotifications: List<UserNotificationUiModel>
  ): List<UserNotificationUiModel> {
    val loadedIds = loadedNotifications.map { it.id }.toSet()
    val currentOnlyNotifications = _uiState.value.notifications.filterNot { it.id in loadedIds }

    return loadedNotifications + currentOnlyNotifications
  }
}

private fun Throwable.toUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "Necesitas volver a iniciar sesion para ver tus notificaciones."
            else -> "No pudimos cargar tus notificaciones. Intenta nuevamente."
          }
      else -> "No pudimos cargar tus notificaciones. Intenta nuevamente."
    }

private fun Throwable.toReportAttributes(): Map<String, String> =
    when (this) {
      is AppApiException ->
          mapOf(
              "error_source" to "app_api",
              "status_code" to statusCode.toString(),
          )
      else -> mapOf("error_source" to "unexpected")
    }
