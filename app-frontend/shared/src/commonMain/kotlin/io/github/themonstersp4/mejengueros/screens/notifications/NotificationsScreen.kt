package io.github.themonstersp4.mejengueros.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.notifications.NotificationsUiState
import io.github.themonstersp4.mejengueros.presentation.notifications.UserNotificationUiModel
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosInlineLoadingState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPill
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPillStyle

data class NotificationsScreenActions(
    val onRetryLoad: () -> Unit,
    val onNotificationSelected: (UserNotificationUiModel) -> Unit,
)

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    contentPadding: PaddingValues,
    actions: NotificationsScreenActions,
    modifier: Modifier = Modifier,
) {
  when {
    state.isLoading ->
        NotificationsLoadingContent(contentPadding = contentPadding, modifier = modifier)
    state.loadErrorMessage != null ->
        NotificationsErrorContent(
            message = state.loadErrorMessage,
            contentPadding = contentPadding,
            onRetryLoad = actions.onRetryLoad,
            modifier = modifier,
        )
    state.isEmpty -> NotificationsEmptyContent(contentPadding = contentPadding, modifier = modifier)
    else ->
        NotificationsListContent(
            state = state,
            contentPadding = contentPadding,
            actions = actions,
            modifier = modifier,
        )
  }
}

@Composable
private fun NotificationsLoadingContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.Center,
  ) {
    MejenguerosInlineLoadingState(
        text = "Cargando notificaciones...",
        modifier = Modifier.fillMaxWidth().testTag("notifications_loading"),
        indicatorTestTag = "notifications_loading_indicator",
    )
  }
}

@Composable
private fun NotificationsErrorContent(
    message: String,
    contentPadding: PaddingValues,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding),
      verticalArrangement = Arrangement.Center,
  ) {
    MejenguerosStateContent(
        title = "No pudimos cargar tus notificaciones",
        description = message,
        variant = MejenguerosStateVariant.Error,
        actions = { MejenguerosFullWidthPrimaryButton(text = "REINTENTAR", onClick = onRetryLoad) },
    )
  }
}

@Composable
private fun NotificationsEmptyContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding),
      verticalArrangement = Arrangement.Center,
  ) {
    MejenguerosStateContent(
        title = "Sin notificaciones",
        description = "Cuando una reserva finalice, te avisaremos para dejar la resena.",
        variant = MejenguerosStateVariant.Empty,
    )
  }
}

@Composable
private fun NotificationsListContent(
    state: NotificationsUiState,
    contentPadding: PaddingValues,
    actions: NotificationsScreenActions,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .verticalScroll(rememberScrollState())
              .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    NotificationsHeader(unreadCount = state.unreadCount)
    state.realtimeMessage?.let { message -> RealtimeNotice(message = message) }
    state.notifications.forEach { notification ->
      NotificationCard(notification = notification, actions = actions)
    }
  }
}

@Composable
private fun NotificationsHeader(unreadCount: Int) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
        text = "Tus avisos",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text =
            if (unreadCount == 1) "Tenes 1 aviso pendiente"
            else "Tenes $unreadCount avisos pendientes",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun RealtimeNotice(message: String) {
  Surface(
      modifier = Modifier.fillMaxWidth().testTag("notifications_realtime_notice"),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.primaryContainer,
  ) {
    Text(
        text = "Nuevo aviso: $message",
        modifier = Modifier.padding(14.dp),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}

@Composable
private fun NotificationCard(
    notification: UserNotificationUiModel,
    actions: NotificationsScreenActions,
) {
  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .clickable { actions.onNotificationSelected(notification) }
              .testTag("notification_card_${notification.id}"),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MejenguerosStatusPill(
            text = if (notification.isRead) "Leida" else "Pendiente",
            style =
                if (notification.isRead) MejenguerosStatusPillStyle.Subtle
                else MejenguerosStatusPillStyle.Primary,
        )
      }
      Text(
          text = notification.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text = notification.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
          text = "Toca para dejar la resena",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}
