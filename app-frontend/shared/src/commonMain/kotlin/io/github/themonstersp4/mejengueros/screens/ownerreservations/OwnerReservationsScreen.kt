package io.github.themonstersp4.mejengueros.screens.ownerreservations

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.OwnerReservationCourtFilter
import io.github.themonstersp4.mejengueros.presentation.ownerreservations.OwnerReservationCardUiModel
import io.github.themonstersp4.mejengueros.presentation.ownerreservations.OwnerReservationsUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosInlineLoadingState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSelectField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosThumbnail

data class OwnerReservationsScreenActions(
    val onCourtSelected: (String?) -> Unit,
    val onRetryLoad: () -> Unit,
)

private const val AllCourtsOptionLabel = "Todas las canchas"

@Composable
fun OwnerReservationsScreen(
    state: OwnerReservationsUiState,
    contentPadding: PaddingValues,
    actions: OwnerReservationsScreenActions,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding).testTag("owner_reservations_root"),
  ) {
    OwnerReservationsCourtFilter(
        availableCourts = state.availableCourts,
        selectedCourtId = state.selectedCourtId,
        onCourtSelected = actions.onCourtSelected,
    )

    when {
      state.isLoading -> OwnerReservationsLoadingContent(modifier = Modifier.fillMaxSize())
      state.loadErrorMessage != null ->
          OwnerReservationsErrorContent(
              message = state.loadErrorMessage,
              onRetryLoad = actions.onRetryLoad,
              modifier = Modifier.fillMaxSize(),
          )
      state.isEmpty -> OwnerReservationsEmptyContent(modifier = Modifier.fillMaxSize())
      else -> OwnerReservationsListContent(state = state, modifier = Modifier.fillMaxSize())
    }
  }
}

@Composable
private fun OwnerReservationsCourtFilter(
    availableCourts: List<OwnerReservationCourtFilter>,
    selectedCourtId: String?,
    onCourtSelected: (String?) -> Unit,
) {
  val selectedLabel =
      availableCourts.firstOrNull { it.courtId == selectedCourtId }?.name ?: AllCourtsOptionLabel
  val options = listOf(AllCourtsOptionLabel) + availableCourts.map { it.name }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 12.dp)
              .testTag("owner_reservations_filter"),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    MejenguerosSelectField(
        value = selectedLabel,
        label = "Cancha",
        options = options,
        onOptionSelected = { option ->
          val courtId =
              if (option == AllCourtsOptionLabel) {
                null
              } else {
                availableCourts.firstOrNull { it.name == option }?.courtId
              }
          onCourtSelected(courtId)
        },
        enabled = availableCourts.isNotEmpty(),
    )
  }
}

@Composable
private fun OwnerReservationsLoadingContent(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.Center,
  ) {
    MejenguerosInlineLoadingState(
        text = "Cargando las reservas de tus canchas...",
        modifier = Modifier.fillMaxWidth().testTag("owner_reservations_loading"),
        indicatorTestTag = "owner_reservations_loading_indicator",
    )
  }
}

@Composable
private fun OwnerReservationsErrorContent(
    message: String,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
    MejenguerosStateContent(
        title = "No pudimos cargar las reservas",
        description = message,
        variant = MejenguerosStateVariant.Error,
        actions = { MejenguerosFullWidthPrimaryButton(text = "REINTENTAR", onClick = onRetryLoad) },
    )
  }
}

@Composable
private fun OwnerReservationsEmptyContent(modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
    MejenguerosStateContent(
        title = "Todavía no hay reservas",
        description =
            "Cuando los mejengueros reserven tus canchas, vas a ver aquí las próximas reservas y el historial finalizado.",
        variant = MejenguerosStateVariant.Empty,
    )
  }
}

@Composable
private fun OwnerReservationsListContent(
    state: OwnerReservationsUiState,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.verticalScroll(rememberScrollState()).padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    if (state.upcoming.isNotEmpty()) {
      OwnerReservationsSection(title = "Próximas") {
        state.upcoming.forEach { card -> OwnerReservationCard(card = card) }
      }
    }

    if (state.finalized.isNotEmpty()) {
      OwnerReservationsSection(title = "Finalizadas") {
        state.finalized.forEach { card -> OwnerReservationCard(card = card) }
      }
    }
  }
}

@Composable
private fun OwnerReservationsSection(
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
private fun OwnerReservationCard(card: OwnerReservationCardUiModel) {
  Surface(
      modifier = Modifier.fillMaxWidth().testTag("owner_reservation_card_${card.id}"),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MejenguerosThumbnail(
          imageUrl = card.imageUrl,
          contentDescription = card.courtName,
          size = DpSize(56.dp, 56.dp),
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
  }
}
