package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

private val ReviewRatingSelectedColor = Color(0xFFF5A623)

@Composable
fun MejenguerosRating(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxRating: Int = 5,
    selectedColor: Color = ReviewRatingSelectedColor,
    unselectedColor: Color = MaterialTheme.colorScheme.outlineVariant,
    showValueLabel: Boolean = true,
) {
  val safeMaxRating = maxRating.coerceAtLeast(1)
  val safeValue = value.coerceIn(0, safeMaxRating)

  Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      (1..safeMaxRating).forEach { rating ->
        val selected = rating <= safeValue
        IconButton(
            onClick = { onValueChange(rating) },
            enabled = enabled,
            modifier =
                Modifier.size(44.dp).semantics {
                  contentDescription =
                      when {
                        !enabled && selected -> "$rating de $safeMaxRating estrellas"
                        !enabled -> "$rating de $safeMaxRating estrellas sin seleccionar"
                        selected -> "$rating de $safeMaxRating estrellas seleccionado"
                        else -> "Seleccionar $rating de $safeMaxRating estrellas"
                      }
                },
        ) {
          Icon(
              imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.Star,
              contentDescription = null,
              modifier = Modifier.size(36.dp),
              tint = if (selected) selectedColor else unselectedColor,
          )
        }
      }
    }

    if (showValueLabel) {
      Text(
          text = ratingLabel(safeValue, safeMaxRating),
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color =
              if (enabled) MaterialTheme.colorScheme.onSurface
              else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun MejenguerosReviewContextCard(
    title: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    imageContentDescription: String? = null,
    metadata: String? = null,
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainer,
      contentColor = MaterialTheme.colorScheme.onSurface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      MejenguerosThumbnail(
          imageUrl = imageUrl,
          contentDescription = imageContentDescription,
          size = DpSize(44.dp, 44.dp),
          shape = MaterialTheme.shapes.small,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!metadata.isNullOrBlank()) {
          Text(
              text = metadata,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
fun MejenguerosReviewCommentField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Comentario",
    optionalLabel: String = "Opcional",
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    placeholderLabel: String = "Cuéntanos cómo estuvo la cancha, la atención o el partido",
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text = label.uppercase(),
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (optionalLabel.isNotBlank()) {
        Text(
            text = optionalLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    MejenguerosTextArea(
        value = value,
        onValueChange = onValueChange,
        label = placeholderLabel,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        minLines = 4,
        maxLines = 6,
    )
  }
}

private fun ratingLabel(value: Int, maxRating: Int): String =
    if (value == 0) "Sin calificación" else "$value de $maxRating estrellas"
