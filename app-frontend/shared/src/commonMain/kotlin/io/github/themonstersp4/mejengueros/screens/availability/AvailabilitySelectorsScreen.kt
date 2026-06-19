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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBottomActionBar
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosDateChipRow
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotGrid
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSlotUiModel
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTimeRangeFields
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosWeekdayChipRow

@Composable
fun AvailabilitySelectorsScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  var selectedDays by rememberSaveable { mutableStateOf(setOf("Lu", "Mi", "Vi")) }
  var selectedDateIndex by rememberSaveable { mutableStateOf(0) }
  var selectedSlotId by rememberSaveable { mutableStateOf("07") }
  var pendingMessage by rememberSaveable { mutableStateOf<String?>(null) }
  var startTime by rememberSaveable { mutableStateOf("06:00") }
  var endTime by rememberSaveable { mutableStateOf("22:00") }
  val days = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do")
  val dates = listOf("Hoy" to "16", "Mar" to "17", "Mié" to "18", "Jue" to "19")
  val timeOptions = listOf("06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "22:00")
  val slotOptions =
      listOf(
          "06" to "06:00",
          "07" to "07:00",
          "08" to "08:00",
          "09" to "09:00",
          "10" to "10:00",
          "11" to "11:00",
      )
  val slots =
      slotOptions.map { (id, label) ->
        val state =
            when {
              id == selectedSlotId -> MejenguerosSlotState.Selected
              id == "09" -> MejenguerosSlotState.Occupied
              id == "11" -> MejenguerosSlotState.Unavailable
              else -> MejenguerosSlotState.Available
            }
        MejenguerosSlotUiModel(id = id, label = label, state = state)
      }
  val previewSlots =
      listOf(
          MejenguerosSlotUiModel("p06", "06:00", MejenguerosSlotState.Preview),
          MejenguerosSlotUiModel("p07", "07:00", MejenguerosSlotState.Preview),
          MejenguerosSlotUiModel("p08", "08:00", MejenguerosSlotState.Preview),
      )

  Column(modifier = modifier.fillMaxSize().padding(contentPadding)) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Text(
          text = "Selectores de disponibilidad",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text =
              "Demo estática para validar componentes reutilizables. No guarda disponibilidad ni crea reservas reales.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      SelectorSection(title = "Días reservables") {
        MejenguerosWeekdayChipRow(
            days = days,
            selectedDays = selectedDays,
            onDayClick = { day ->
              selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
            },
        )
      }

      SelectorSection(title = "Horario de operación") {
        MejenguerosTimeRangeFields(
            startTime = startTime,
            endTime = endTime,
            timeOptions = timeOptions,
            startLabel = "Apertura",
            endLabel = "Cierre",
            onStartSelected = { selected ->
              startTime = selected
              pendingMessage = "Hora de apertura seleccionada localmente."
            },
            onEndSelected = { selected ->
              endTime = selected
              pendingMessage = "Hora de cierre seleccionada localmente."
            },
        )
      }

      SelectorSection(title = "Vista previa de slots") {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
                text =
                    "Ejemplo visual para validar estados de selección, ocupación y vista previa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MejenguerosSlotGrid(slots = previewSlots, onSlotSelected = {}, columns = 3)
          }
        }
      }

      SelectorSection(title = "Reserva de slot") {
        MejenguerosDateChipRow(
            dates = dates,
            selectedIndex = selectedDateIndex,
            onDateSelected = { selectedDateIndex = it },
        )
        Spacer(modifier = Modifier.height(10.dp))
        MejenguerosSlotGrid(slots = slots, onSlotSelected = { selectedSlotId = it })
        Text(
            text = "Los horarios ocupados o no disponibles quedan bloqueados visualmente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      pendingMessage?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    MejenguerosBottomActionBar {
      Text(
          text =
              "Selección local de ejemplo: ${slotOptions.first { it.first == selectedSlotId }.second}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      MejenguerosFullWidthPrimaryButton(
          text = "Validar selección local",
          onClick = {
            pendingMessage = "Esta pantalla sólo valida componentes visuales; no guarda datos."
          },
      )
    }
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
