package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

enum class MejenguerosSlotState {
  Available,
  Selected,
  Occupied,
  Unavailable,
  Preview,
}

data class MejenguerosSlotUiModel(
    val id: String,
    val label: String,
    val state: MejenguerosSlotState,
)

@Composable
fun MejenguerosWeekdayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val containerColor =
      if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
  val contentColor =
      if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  val borderColor =
      if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

  Surface(
      modifier = modifier,
      shape = CircleShape,
      color = containerColor,
      contentColor = contentColor,
      border = BorderStroke(1.dp, borderColor),
      onClick = onClick,
      enabled = enabled,
  ) {
    Text(
        text = label,
        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun MejenguerosWeekdayChipRow(
    days: List<String>,
    selectedDays: Set<String>,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    days.forEach { day ->
      MejenguerosWeekdayChip(
          label = day,
          selected = day in selectedDays,
          onClick = { onDayClick(day) },
          modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
fun MejenguerosDateChip(
    dayLabel: String,
    dateLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  val containerColor =
      if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
  val contentColor =
      if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  val borderColor =
      if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

  Surface(
      modifier = modifier.widthIn(min = 60.dp),
      shape = RoundedCornerShape(18.dp),
      color = containerColor,
      contentColor = contentColor,
      border = BorderStroke(1.dp, borderColor),
      onClick = onClick,
      enabled = enabled,
  ) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(text = dayLabel, style = MaterialTheme.typography.labelMedium)
      Text(
          text = dateLabel,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      )
    }
  }
}

@Composable
fun MejenguerosDateChipRow(
    dates: List<Pair<String, String>>,
    selectedIndex: Int,
    onDateSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    dates.forEachIndexed { index, date ->
      MejenguerosDateChip(
          dayLabel = date.first,
          dateLabel = date.second,
          selected = selectedIndex == index,
          onClick = { onDateSelected(index) },
          modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
fun MejenguerosSlotChip(
    slot: MejenguerosSlotUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val selected = slot.state == MejenguerosSlotState.Selected
  val disabled =
      slot.state == MejenguerosSlotState.Occupied || slot.state == MejenguerosSlotState.Unavailable
  val preview = slot.state == MejenguerosSlotState.Preview
  val containerColor =
      when {
        selected -> MaterialTheme.colorScheme.primary
        disabled -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
      }
  val contentColor =
      when {
        selected -> MaterialTheme.colorScheme.onPrimary
        disabled -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
      }
  val borderColor =
      if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  val shape = RoundedCornerShape(14.dp)

  Text(
      text = slot.label,
      modifier =
          modifier
              .height(48.dp)
              .clip(shape)
              .background(containerColor, shape)
              .border(1.dp, borderColor, shape)
              .clickable(enabled = !disabled && !preview, onClick = onClick)
              .padding(horizontal = 8.dp, vertical = 14.dp),
      color = contentColor,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      textAlign = TextAlign.Center,
      textDecoration = if (disabled) TextDecoration.LineThrough else TextDecoration.None,
  )
}

@Composable
fun MejenguerosSlotGrid(
    slots: List<MejenguerosSlotUiModel>,
    onSlotSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    slots.chunked(columns).forEach { rowSlots ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        rowSlots.forEach { slot ->
          MejenguerosSlotChip(
              slot = slot,
              onClick = { onSlotSelected(slot.id) },
              modifier = Modifier.weight(1f),
          )
        }
        repeat(columns - rowSlots.size) { Text(text = "", modifier = Modifier.weight(1f)) }
      }
    }
  }
}

@Composable
fun MejenguerosTimeRangeFields(
    startTime: String,
    endTime: String,
    modifier: Modifier = Modifier,
    onStartClick: () -> Unit = {},
    onEndClick: () -> Unit = {},
) {
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    MejenguerosSelectField(
        value = startTime,
        label = "Apertura",
        onClick = onStartClick,
        modifier = Modifier.weight(1f),
    )
    MejenguerosSelectField(
        value = endTime,
        label = "Cierre",
        onClick = onEndClick,
        modifier = Modifier.weight(1f),
    )
  }
}
