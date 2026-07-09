package io.github.themonstersp4.mejengueros.screens.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import io.github.themonstersp4.mejengueros.presentation.myreservations.MyReservationCardUiModel
import io.github.themonstersp4.mejengueros.presentation.myreservations.MyReservationsUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosButtonSize
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosInlineLoadingState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPill
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPillStyle
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosThumbnail

data class MyReservationsScreenActions(
    val onRetryLoad: () -> Unit,
    val onReviewRequested: (MyReservationCardUiModel) -> Unit,
)

private const val SupportedPrimaryActionKey = "leave_review"

@Composable
fun MyReservationsScreen(
    state: MyReservationsUiState,
    contentPadding: PaddingValues,
    actions: MyReservationsScreenActions,
    modifier: Modifier = Modifier,
) {
  when {
    state.isLoading ->
        MyReservationsLoadingContent(contentPadding = contentPadding, modifier = modifier)
    state.loadErrorMessage != null ->
        MyReservationsErrorContent(
            message = state.loadErrorMessage,
            contentPadding = contentPadding,
            onRetryLoad = actions.onRetryLoad,
            modifier = modifier,
        )
    state.isEmpty ->
        MyReservationsEmptyContent(contentPadding = contentPadding, modifier = modifier)
    else ->
        MyReservationsListContent(
            state = state,
            contentPadding = contentPadding,
            actions = actions,
            modifier = modifier,
        )
  }
}

@Composable
private fun MyReservationsLoadingContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.Center,
  ) {
    MejenguerosInlineLoadingState(
        text = "Cargando tus reservas...",
        modifier = Modifier.fillMaxWidth().testTag("my_reservations_loading"),
        indicatorTestTag = "my_reservations_loading_indicator",
    )
  }
}

@Composable
private fun MyReservationsErrorContent(
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
        title = "No pudimos cargar tus reservas",
        description = message,
        variant = MejenguerosStateVariant.Error,
        actions = { MejenguerosFullWidthPrimaryButton(text = "REINTENTAR", onClick = onRetryLoad) },
    )
  }
}

@Composable
private fun MyReservationsEmptyContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding),
      verticalArrangement = Arrangement.Center,
  ) {
    MejenguerosStateContent(
        title = "Todavía no tenés reservas",
        description =
            "Cuando reserves una cancha, vas a ver aquí tus próximas mejengas y el historial finalizado.",
        variant = MejenguerosStateVariant.Empty,
    )
  }
}

@Composable
private fun MyReservationsListContent(
    state: MyReservationsUiState,
    contentPadding: PaddingValues,
    actions: MyReservationsScreenActions,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .verticalScroll(rememberScrollState())
              .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    if (state.upcoming.isNotEmpty()) {
      ReservationsSection(title = "Próximas") {
        state.upcoming.forEach { card -> MyReservationCard(card = card, actions = actions) }
      }
    }

    if (state.finalized.isNotEmpty()) {
      ReservationsSection(title = "Finalizadas") {
        state.finalized.forEach { card -> MyReservationCard(card = card, actions = actions) }
      }
    }
  }
}

@Composable
private fun ReservationsSection(
    title: String,
    content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = { content() })
  }
}

@Composable
private fun MyReservationCard(
    card: MyReservationCardUiModel,
    actions: MyReservationsScreenActions,
) {
  Surface(
      modifier = Modifier.fillMaxWidth().testTag("my_reservation_card_${card.id}"),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      androidx.compose.foundation.layout.Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        MejenguerosThumbnail(
            imageUrl = card.imageUrl,
            contentDescription = card.courtName,
            size = androidx.compose.ui.unit.DpSize(56.dp, 56.dp),
            shape = MaterialTheme.shapes.medium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
              text = card.title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
              text = card.reservationLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      card.indicatorLabel?.let { label ->
        MejenguerosStatusPill(
            text = label,
            style = MejenguerosStatusPillStyle.Subtle,
            modifier = Modifier.testTag("my_reservation_indicator_${card.id}"),
        )
      }

      card.primaryActionLabel
          ?.takeIf { card.primaryActionKey == SupportedPrimaryActionKey }
          ?.let { label ->
            MejenguerosFullWidthPrimaryButton(
                text = label,
                onClick = { actions.onReviewRequested(card) },
                size = MejenguerosButtonSize.Compact,
                modifier = Modifier.testTag("my_reservation_action_${card.id}"),
            )
          }
    }
  }
}
