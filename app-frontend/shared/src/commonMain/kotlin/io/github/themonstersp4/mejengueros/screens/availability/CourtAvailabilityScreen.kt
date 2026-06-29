package io.github.themonstersp4.mejengueros.screens.availability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityWeekday
import io.github.themonstersp4.mejengueros.presentation.availability.CourtAvailabilityUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBottomActionBar
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosConfirmationDialog
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotGrid
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotUiModel
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSupportingText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTimeRangeFields
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosWeekdayChipRow

data class CourtAvailabilityScreenActions(
    val onToggleDay: (CourtAvailabilityWeekday) -> Unit,
    val onStartTimeSelected: (String) -> Unit,
    val onEndTimeSelected: (String) -> Unit,
    val onRetry: () -> Unit,
    val onSave: () -> Unit,
    val onSuccessAcknowledged: () -> Unit,
)

private val availabilityDays =
    listOf(
        CourtAvailabilityWeekday.MONDAY,
        CourtAvailabilityWeekday.TUESDAY,
        CourtAvailabilityWeekday.WEDNESDAY,
        CourtAvailabilityWeekday.THURSDAY,
        CourtAvailabilityWeekday.FRIDAY,
        CourtAvailabilityWeekday.SATURDAY,
        CourtAvailabilityWeekday.SUNDAY,
    )

private val timeOptions = (0..23).map { hour -> hour.toString().padStart(2, '0') + ":00" }

@Composable
fun CourtAvailabilityScreen(
    state: CourtAvailabilityUiState,
    contentPadding: PaddingValues,
    actions: CourtAvailabilityScreenActions,
    modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(contentPadding)) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Text(
          text = "Configurá horarios de reserva",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
      )
      if (state.courtName.isNotBlank() || state.complexName.isNotBlank()) {
        Text(
            text =
                listOf(state.courtName, state.complexName)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
      }
      MejenguerosSupportingText(
          text = "Elegí los días y el rango horario único para generar slots de 1 hora.",
      )

      if (state.isLoading) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
              text = "Cargando disponibilidad...",
              modifier = Modifier.padding(16.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        SelectorSection(title = "Días disponibles") {
          MejenguerosSupportingText(text = "Los días sin marcar quedan cerrados.")
          MejenguerosWeekdayChipRow(
              days = availabilityDays.map(CourtAvailabilityWeekday::toShortLabel),
              selectedDays = state.selectedDays.map(CourtAvailabilityWeekday::toShortLabel).toSet(),
              onDayClick = { selectedLabel ->
                availabilityDays
                    .firstOrNull { it.toShortLabel() == selectedLabel }
                    ?.let(actions.onToggleDay)
              },
          )
        }

        SelectorSection(title = "Rango horario") {
          MejenguerosSupportingText(
              text = "El sistema genera slots exactos de 1 hora dentro de este rango.",
          )
          MejenguerosTimeRangeFields(
              startTime = state.startTime,
              endTime = state.endTime,
              timeOptions = timeOptions,
              onStartSelected = actions.onStartTimeSelected,
              onEndSelected = actions.onEndTimeSelected,
              startLabel = "Apertura",
              endLabel = "Cierre",
          )
        }

        SelectorSection(title = "Slots que se generan") {
          Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              val previewText =
                  if (state.previewSlots.isEmpty()) {
                    "Elegí un rango horario exacto de una hora o más para ver la vista previa."
                  } else {
                    "${state.previewSlots.size} slots de 1 h por día seleccionado"
                  }
              Text(
                  text = previewText,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (state.previewSlots.isNotEmpty()) {
                MejenguerosSlotGrid(
                    slots =
                        state.previewSlots.map { slot ->
                          MejenguerosSlotUiModel(
                              id = slot,
                              label = slot,
                              state = MejenguerosSlotState.Preview,
                          )
                        },
                    onSlotSelected = {},
                    columns = 3,
                )
              }
            }
          }
        }
      }

      state.errorMessage?.let { message ->
        MejenguerosErrorText(text = message)
        MejenguerosFullWidthOutlinedButton(
            text = if (state.isLoading) "Reintentando..." else "Reintentar",
            onClick = actions.onRetry,
            enabled = !state.isLoading && !state.isSaving,
        )
      }
    }

    MejenguerosBottomActionBar {
      Text(
          text = "Guardá la disponibilidad base para habilitar reservas por cancha.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      MejenguerosFullWidthPrimaryButton(
          text = if (state.isSaving) "Guardando..." else "Guardar disponibilidad",
          onClick = actions.onSave,
          enabled = state.canSave,
      )
    }
  }

  state.successMessage?.let { message ->
    MejenguerosConfirmationDialog(
        title = "Disponibilidad configurada",
        message = message,
        confirmText = "Ir a Mi complejo",
        onConfirm = actions.onSuccessAcknowledged,
        onDismissRequest = {},
    )
  }
}

@Composable
private fun SelectorSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    content()
  }
}

private fun CourtAvailabilityWeekday.toShortLabel(): String =
    when (this) {
      CourtAvailabilityWeekday.MONDAY -> "Lu"
      CourtAvailabilityWeekday.TUESDAY -> "Ma"
      CourtAvailabilityWeekday.WEDNESDAY -> "Mi"
      CourtAvailabilityWeekday.THURSDAY -> "Ju"
      CourtAvailabilityWeekday.FRIDAY -> "Vi"
      CourtAvailabilityWeekday.SATURDAY -> "Sa"
      CourtAvailabilityWeekday.SUNDAY -> "Do"
    }
