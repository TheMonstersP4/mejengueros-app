package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SelectorChipSpacing = 8.dp
private val WeekdayChipMinWidth = 44.dp
private val DateChipMinWidth = 60.dp

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
  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val useScrollableLayout =
        shouldUseScrollableSelectorRow(days.size, WeekdayChipMinWidth, maxWidth)

    Row(
        modifier = selectorRowModifier(useScrollableLayout),
        horizontalArrangement = Arrangement.spacedBy(SelectorChipSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      days.forEach { day ->
        MejenguerosWeekdayChip(
            label = day,
            selected = day in selectedDays,
            onClick = { onDayClick(day) },
            modifier = selectorChipModifier(useScrollableLayout, WeekdayChipMinWidth),
        )
      }
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
      modifier = modifier.widthIn(min = DateChipMinWidth),
      shape = MaterialTheme.shapes.large,
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
  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val useScrollableLayout = shouldUseScrollableSelectorRow(dates.size, DateChipMinWidth, maxWidth)

    Row(
        modifier = selectorRowModifier(useScrollableLayout),
        horizontalArrangement = Arrangement.spacedBy(SelectorChipSpacing),
    ) {
      dates.forEachIndexed { index, date ->
        MejenguerosDateChip(
            dayLabel = date.first,
            dateLabel = date.second,
            selected = selectedIndex == index,
            onClick = { onDateSelected(index) },
            modifier = selectorChipModifier(useScrollableLayout, DateChipMinWidth),
        )
      }
    }
  }
}

private fun shouldUseScrollableSelectorRow(
    itemCount: Int,
    chipMinWidth: Dp,
    availableWidth: Dp,
): Boolean {
  if (itemCount <= 1) return false
  val requiredWidth = (chipMinWidth * itemCount) + (SelectorChipSpacing * (itemCount - 1))
  return requiredWidth > availableWidth
}

@Composable
private fun selectorRowModifier(useScrollableLayout: Boolean): Modifier =
    if (useScrollableLayout) {
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    } else {
      Modifier.fillMaxWidth()
    }

private fun RowScope.selectorChipModifier(useScrollableLayout: Boolean, minWidth: Dp): Modifier =
    if (useScrollableLayout) {
      Modifier.widthIn(min = minWidth)
    } else {
      Modifier.weight(1f).widthIn(min = minWidth)
    }

@Composable
fun MejenguerosSlotChip(
    slot: MejenguerosSlotUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val style = slot.state.style()
  val shape = MaterialTheme.shapes.medium

  Text(
      text = slot.label,
      modifier =
          modifier
              .height(48.dp)
              .clip(shape)
              .background(style.containerColor, shape)
              .border(1.dp, style.borderColor, shape)
              .clickable(enabled = style.enabled, onClick = onClick)
              .padding(horizontal = 8.dp, vertical = 14.dp),
      color = style.contentColor,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      textAlign = TextAlign.Center,
      textDecoration = style.textDecoration,
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
    timeOptions: List<String>,
    onStartSelected: (String) -> Unit,
    onEndSelected: (String) -> Unit,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    MejenguerosSelectField(
        value = startTime,
        label = startLabel,
        options = timeOptions,
        onOptionSelected = onStartSelected,
        modifier = Modifier.weight(1f),
    )
    MejenguerosSelectField(
        value = endTime,
        label = endLabel,
        options = timeOptions,
        onOptionSelected = onEndSelected,
        modifier = Modifier.weight(1f),
    )
  }
}

private data class SlotStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val enabled: Boolean,
    val textDecoration: TextDecoration = TextDecoration.None,
)

@Composable
private fun MejenguerosSlotState.style(): SlotStyle =
    when (this) {
      MejenguerosSlotState.Available ->
          SlotStyle(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface,
              borderColor = MaterialTheme.colorScheme.outlineVariant,
              enabled = true,
          )
      MejenguerosSlotState.Selected ->
          SlotStyle(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              borderColor = MaterialTheme.colorScheme.primary,
              enabled = true,
          )
      MejenguerosSlotState.Occupied ->
          SlotStyle(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              borderColor = MaterialTheme.colorScheme.outlineVariant,
              enabled = false,
              textDecoration = TextDecoration.LineThrough,
          )
      MejenguerosSlotState.Unavailable ->
          SlotStyle(
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer,
              borderColor = MaterialTheme.colorScheme.error,
              enabled = false,
              textDecoration = TextDecoration.LineThrough,
          )
      MejenguerosSlotState.Preview ->
          SlotStyle(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              borderColor = MaterialTheme.colorScheme.primaryContainer,
              enabled = false,
          )
    }
