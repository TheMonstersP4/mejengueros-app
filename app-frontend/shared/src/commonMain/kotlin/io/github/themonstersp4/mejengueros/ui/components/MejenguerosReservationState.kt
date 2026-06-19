package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Theme-driven variants for result, empty, and pending states. */
enum class MejenguerosStateVariant {
  Success,
  Error,
  Empty,
  Pending,
}

data class MejenguerosTicketSummaryRow(
    val label: String,
    val value: String,
    val supportingText: String? = null,
)

@Composable
fun MejenguerosStateContent(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    variant: MejenguerosStateVariant = MejenguerosStateVariant.Empty,
    indicator: (@Composable RowScope.() -> Unit)? = null,
    body: (@Composable ColumnScope.() -> Unit)? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
  val colors = mejenguerosStateColors(variant)

  Column(
      modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Surface(
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.content,
    ) {
      Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (indicator != null) {
            indicator()
          } else {
            Text(
                text = defaultIndicatorText(variant),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
          }
        }
      }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
      )
      Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
      )
    }

    if (body != null) {
      Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          content = body,
      )
    }

    if (actions != null) {
      Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          content = actions,
      )
    }
  }
}

@Composable
fun MejenguerosTicketSummary(
    rows: List<MejenguerosTicketSummaryRow>,
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      if (!title.isNullOrBlank()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
      }

      rows.forEachIndexed { index, row ->
        MejenguerosTicketSummaryItem(row = row)
        if (index < rows.lastIndex) {
          HorizontalDivider(
              modifier = Modifier.padding(vertical = 4.dp),
              color = MaterialTheme.colorScheme.outlineVariant,
          )
        }
      }

      if (footer != null) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = footer)
      }
    }
  }
}

@Composable
fun MejenguerosReservationSummaryBar(
    summary: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
  MejenguerosBottomActionBar(modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = summary,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
      )
      if (!supportingText.isNullOrBlank()) {
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
      }
      MejenguerosFullWidthPrimaryButton(
          text = actionText,
          onClick = onActionClick,
          enabled = enabled,
      )
    }
  }
}

@Composable
private fun MejenguerosTicketSummaryItem(
    row: MejenguerosTicketSummaryRow,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
  ) {
    Text(
        text = row.label,
        modifier = Modifier.weight(0.9f),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(
        modifier = Modifier.weight(1.1f),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
          text = row.value,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.End,
      )
      if (!row.supportingText.isNullOrBlank()) {
        Text(
            text = row.supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
      }
    }
  }
}

private data class MejenguerosStateColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun mejenguerosStateColors(variant: MejenguerosStateVariant): MejenguerosStateColors =
    when (variant) {
      MejenguerosStateVariant.Success ->
          MejenguerosStateColors(
              container = MaterialTheme.colorScheme.primaryContainer,
              content = MaterialTheme.colorScheme.onPrimaryContainer,
          )
      MejenguerosStateVariant.Error ->
          MejenguerosStateColors(
              container = MaterialTheme.colorScheme.errorContainer,
              content = MaterialTheme.colorScheme.onErrorContainer,
          )
      MejenguerosStateVariant.Empty ->
          MejenguerosStateColors(
              container = MaterialTheme.colorScheme.surfaceVariant,
              content = MaterialTheme.colorScheme.onSurfaceVariant,
          )
      MejenguerosStateVariant.Pending ->
          MejenguerosStateColors(
              container = MaterialTheme.colorScheme.secondaryContainer,
              content = MaterialTheme.colorScheme.onSecondaryContainer,
          )
    }

private fun defaultIndicatorText(variant: MejenguerosStateVariant): String =
    when (variant) {
      MejenguerosStateVariant.Success -> "✓"
      MejenguerosStateVariant.Error -> "!"
      MejenguerosStateVariant.Empty -> "–"
      MejenguerosStateVariant.Pending -> "…"
    }
