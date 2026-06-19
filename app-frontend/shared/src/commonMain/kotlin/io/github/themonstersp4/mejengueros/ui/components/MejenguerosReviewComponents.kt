package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
        modifier = Modifier.semantics { selectableGroup() },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      (1..safeMaxRating).forEach { rating ->
        val filled = rating <= safeValue
        val isCurrentRating = rating == safeValue
        IconButton(
            onClick = { onValueChange(rating) },
            enabled = enabled,
            modifier =
                Modifier.size(44.dp).semantics {
                  role = Role.RadioButton
                  selected = isCurrentRating
                  contentDescription =
                      when {
                        !enabled && isCurrentRating ->
                            "$rating de $safeMaxRating estrellas seleccionado"
                        !enabled -> "$rating de $safeMaxRating estrellas sin seleccionar"
                        isCurrentRating -> "$rating de $safeMaxRating estrellas seleccionado"
                        else -> "Seleccionar $rating de $safeMaxRating estrellas"
                      }
                },
        ) {
          Icon(
              imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
              contentDescription = null,
              modifier = Modifier.size(36.dp),
              tint = if (filled) selectedColor else unselectedColor,
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
fun MejenguerosCompactRating(
    value: Int,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    selectedColor: Color = ReviewRatingSelectedColor,
    unselectedColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
  val safeMaxRating = maxRating.coerceAtLeast(1)
  val safeValue = value.coerceIn(0, safeMaxRating)

  Row(
      modifier =
          modifier.semantics(mergeDescendants = true) {
            contentDescription = ratingLabel(safeValue, safeMaxRating)
          },
      horizontalArrangement = Arrangement.spacedBy(1.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    (1..safeMaxRating).forEach { rating ->
      val filled = rating <= safeValue
      Icon(
          imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = if (filled) selectedColor else unselectedColor,
      )
    }
  }
}

@Composable
fun MejenguerosReviewSummary(
    reviewCountText: String,
    averageText: String,
    modifier: Modifier = Modifier,
) {
  Text(
      modifier = modifier.fillMaxWidth(),
      text = "$reviewCountText · $averageText",
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
fun MejenguerosReceivedReviewCard(
    author: String,
    date: String,
    rating: Int,
    comment: String,
    modifier: Modifier = Modifier,
    avatarInitials: String? = null,
    maxRating: Int = 5,
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        if (!avatarInitials.isNullOrBlank()) {
          ReceivedReviewAvatar(initials = avatarInitials)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
              text = author,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
          Text(
              text = date,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }
        MejenguerosCompactRating(value = rating, maxRating = maxRating)
      }
      Text(
          text = comment,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun ReceivedReviewAvatar(
    initials: String,
) {
  Surface(
      modifier = Modifier.size(38.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
          text = initials,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          textAlign = TextAlign.Center,
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
          text = label,
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
