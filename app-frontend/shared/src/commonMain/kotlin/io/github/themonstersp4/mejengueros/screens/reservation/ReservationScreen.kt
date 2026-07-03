package io.github.themonstersp4.mejengueros.screens.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationTicketUiModel
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationUiMode
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosDateChipRow
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosReservationSummaryBar
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotGrid
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotUiModel
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSupportingText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTicketSummary
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTicketSummaryRow

data class ReservationScreenActions(
    val onDateSelected: (Int) -> Unit,
    val onSlotSelected: (String) -> Unit,
    val onConfirmReservation: () -> Unit,
    val onRetryLoad: () -> Unit,
    val onViewOtherHours: () -> Unit,
    val onViewReservations: () -> Unit,
    val onReturnToCatalog: () -> Unit,
)

@Composable
fun ReservationScreen(
    state: ReservationUiState,
    contentPadding: PaddingValues,
    actions: ReservationScreenActions,
    modifier: Modifier = Modifier,
) {
  when (val mode = state.mode) {
    ReservationUiMode.Selection ->
        ReservationSelectionContent(
            state = state,
            contentPadding = contentPadding,
            actions = actions,
            modifier = modifier,
        )
    is ReservationUiMode.Success ->
        ReservationSuccessContent(
            ticket = mode.ticket,
            contentPadding = contentPadding,
            onViewReservations = actions.onViewReservations,
            onReturnToCatalog = actions.onReturnToCatalog,
            modifier = modifier,
        )
    is ReservationUiMode.Conflict ->
        ReservationConflictContent(
            attemptedTimeLabel = mode.attemptedTimeLabel,
            contentPadding = contentPadding,
            onViewOtherHours = actions.onViewOtherHours,
            modifier = modifier,
        )
  }
}

@Composable
private fun ReservationSelectionContent(
    state: ReservationUiState,
    contentPadding: PaddingValues,
    actions: ReservationScreenActions,
    modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(contentPadding)) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      ReservationSection(title = "Elegí el día") {
        MejenguerosDateChipRow(
            dates = state.dates.map { date -> date.dayLabel to date.dateLabel },
            selectedIndex = state.selectedDateIndex,
            onDateSelected = actions.onDateSelected,
        )
      }

      ReservationSection(title = "Horarios de 1 hora") {
        MejenguerosSupportingText(
            text = "Los horarios tachados ya no están disponibles para esta cancha.",
        )

        when {
          state.isLoadingSlots -> {
            Text(
                text = "Cargando horarios disponibles...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          state.loadErrorMessage != null -> {
            MejenguerosStateContent(
                title = "No pudimos cargar horarios",
                description = state.loadErrorMessage,
                variant = MejenguerosStateVariant.Error,
                actions = {
                  MejenguerosFullWidthOutlinedButton(
                      text = "Reintentar",
                      onClick = actions.onRetryLoad,
                  )
                },
            )
          }

          state.slots.isEmpty() -> {
            MejenguerosStateContent(
                title = "Sin horarios disponibles",
                description =
                    "No encontramos slots de una hora para la fecha seleccionada. Probá otro día.",
                variant = MejenguerosStateVariant.Empty,
            )
          }

          else -> {
            MejenguerosSlotGrid(
                slots =
                    state.slots.map { slot ->
                      MejenguerosSlotUiModel(id = slot.id, label = slot.label, state = slot.state)
                    },
                onSlotSelected = actions.onSlotSelected,
                modifier = Modifier.testTag("reservation_slot_grid"),
            )
          }
        }
      }
    }

    MejenguerosReservationSummaryBar(
        summary = state.selectionSummary ?: "Elegí una fecha y un horario para continuar",
        supportingText = state.selectionSupportingText ?: "La reserva bloquea exactamente 1 hora.",
        actionText = if (state.isSubmitting) "CONFIRMANDO..." else "CONFIRMAR RESERVA",
        onActionClick = actions.onConfirmReservation,
        enabled = state.canConfirm,
    )
  }
}

@Composable
private fun ReservationSuccessContent(
    ticket: ReservationTicketUiModel,
    contentPadding: PaddingValues,
    onViewReservations: () -> Unit,
    onReturnToCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
      verticalArrangement = Arrangement.Center,
  ) {
    MejenguerosStateContent(
        title = "¡GOOOL! RESERVA CONFIRMADA",
        description =
            "Tu cancha quedó reservada por 1 hora. Te avisaremos cuando termine para dejar tu reseña.",
        variant = MejenguerosStateVariant.Success,
        body = { ReservationTicket(ticket = ticket) },
        actions = {
          MejenguerosFullWidthPrimaryButton(
              text = "VER MIS RESERVAS",
              onClick = onViewReservations,
          )
          MejenguerosFullWidthOutlinedButton(
              text = "VOLVER AL CATÁLOGO",
              onClick = onReturnToCatalog,
          )
        },
    )
  }
}

@Composable
private fun ReservationConflictContent(
    attemptedTimeLabel: String,
    contentPadding: PaddingValues,
    onViewOtherHours: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
      verticalArrangement = Arrangement.Center,
  ) {
    MejenguerosStateContent(
        title = "ESE HORARIO YA SE RESERVÓ",
        description =
            "El slot de $attemptedTimeLabel lo tomó otra persona hace un momento. Elegí otro horario disponible para esta cancha.",
        variant = MejenguerosStateVariant.Error,
        actions = {
          MejenguerosFullWidthPrimaryButton(
              text = "VER OTROS HORARIOS",
              onClick = onViewOtherHours,
          )
        },
    )
  }
}

@Composable
private fun ReservationTicket(ticket: ReservationTicketUiModel) {
  MejenguerosTicketSummary(
      title = "Detalle de reserva",
      rows =
          listOf(
              MejenguerosTicketSummaryRow(
                  label = "Cancha",
                  value = ticket.courtLabel,
                  supportingText = ticket.locationLabel.ifBlank { null },
              ),
              MejenguerosTicketSummaryRow(label = "Fecha", value = ticket.dateLabel),
              MejenguerosTicketSummaryRow(label = "Horario", value = ticket.timeLabel),
          ),
  )
}

@Composable
private fun ReservationSection(
    title: String,
    content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    content()
  }
}
