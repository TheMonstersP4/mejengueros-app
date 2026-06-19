package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Theme-driven visual variants for reusable status labels. */
enum class MejenguerosStatusPillStyle {
  Primary,
  Neutral,
  Subtle,
  Error,
}

@Composable
fun MejenguerosStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    style: MejenguerosStatusPillStyle = MejenguerosStatusPillStyle.Neutral,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
  val colors = mejenguerosStatusPillColors(style)

  Surface(
      modifier = modifier,
      shape = CircleShape,
      color = colors.container,
      contentColor = colors.content,
      border = BorderStroke(1.dp, colors.border),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      if (leading != null) {
        leading()
      }
      Text(
          text = text,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun MejenguerosThumbnail(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: DpSize? = DpSize(72.dp, 72.dp),
    shape: Shape = MaterialTheme.shapes.medium,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable BoxScope.() -> Unit = { MejenguerosThumbnailPlaceholder() },
) {
  val sizedModifier = if (size != null) modifier.size(size) else modifier
  val placeholderSemanticsModifier =
      if (imageUrl.isNullOrBlank() && contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
      } else {
        Modifier
      }
  Box(
      modifier =
          sizedModifier
              .clip(shape)
              .background(MaterialTheme.colorScheme.surfaceContainerHighest)
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
              .then(placeholderSemanticsModifier),
      contentAlignment = Alignment.Center,
  ) {
    if (imageUrl.isNullOrBlank()) {
      placeholder()
    } else {
      AsyncImage(
          model = imageUrl,
          contentDescription = contentDescription,
          contentScale = contentScale,
          modifier = Modifier.matchParentSize(),
      )
    }
  }
}

@Composable
fun MejenguerosListItem(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    showDivider: Boolean = false,
) {
  Column(modifier = modifier) {
    val rowContent: @Composable () -> Unit = {
      Row(
          modifier = Modifier.fillMaxWidth().padding(contentPadding),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        if (leading != null) {
          leading()
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
              text = title,
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              color =
                  if (enabled) MaterialTheme.colorScheme.onSurface
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
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
        }
        if (trailing != null) {
          trailing()
        }
      }
    }

    if (onClick != null) {
      Surface(
          onClick = onClick,
          enabled = enabled,
          color = Color.Transparent,
          contentColor = MaterialTheme.colorScheme.onSurface,
      ) {
        rowContent()
      }
    } else {
      Surface(color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface) {
        rowContent()
      }
    }

    if (showDivider) {
      HorizontalDivider(
          modifier = Modifier.padding(start = if (leading != null) 72.dp else 16.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
      )
    }
  }
}

@Composable
fun MejenguerosListGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(content = content)
  }
}

@Composable
fun MejenguerosCourtCard(
    title: String,
    location: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    imageContentDescription: String? = null,
    metadata: List<String> = emptyList(),
    statusText: String? = null,
    statusStyle: MejenguerosStatusPillStyle = MejenguerosStatusPillStyle.Primary,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
  val cardColors =
      CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
          disabledContainerColor = MaterialTheme.colorScheme.surface,
          disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      )

  val cardContent: @Composable () -> Unit = {
    Column(modifier = Modifier.fillMaxWidth()) {
      Box {
        MejenguerosThumbnail(
            imageUrl = imageUrl,
            contentDescription = imageContentDescription,
            modifier = Modifier.fillMaxWidth().height(154.dp),
            size = null,
            shape = MaterialTheme.shapes.large,
        )
        if (!statusText.isNullOrBlank()) {
          MejenguerosStatusPill(
              text = statusText,
              style = statusStyle,
              modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
          )
        }
      }
      Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = location,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (metadata.isNotEmpty()) {
          Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            metadata.forEach { label ->
              MejenguerosStatusPill(
                  text = label,
                  style = MejenguerosStatusPillStyle.Subtle,
              )
            }
          }
        }
      }
    }
  }

  if (onClick != null) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = cardColors,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      cardContent()
    }
  } else {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = cardColors,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      cardContent()
    }
  }
}

private data class MejenguerosStatusPillColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

@Composable
private fun mejenguerosStatusPillColors(
    style: MejenguerosStatusPillStyle
): MejenguerosStatusPillColors =
    when (style) {
      MejenguerosStatusPillStyle.Primary ->
          MejenguerosStatusPillColors(
              container = MaterialTheme.colorScheme.primaryContainer,
              content = MaterialTheme.colorScheme.onPrimaryContainer,
              border = MaterialTheme.colorScheme.primaryContainer,
          )
      MejenguerosStatusPillStyle.Neutral ->
          MejenguerosStatusPillColors(
              container = MaterialTheme.colorScheme.surface,
              content = MaterialTheme.colorScheme.onSurface,
              border = MaterialTheme.colorScheme.outlineVariant,
          )
      MejenguerosStatusPillStyle.Subtle ->
          MejenguerosStatusPillColors(
              container = MaterialTheme.colorScheme.surfaceVariant,
              content = MaterialTheme.colorScheme.onSurfaceVariant,
              border = MaterialTheme.colorScheme.surfaceVariant,
          )
      MejenguerosStatusPillStyle.Error ->
          MejenguerosStatusPillColors(
              container = MaterialTheme.colorScheme.errorContainer,
              content = MaterialTheme.colorScheme.onErrorContainer,
              border = MaterialTheme.colorScheme.errorContainer,
          )
    }

@Composable
private fun MejenguerosThumbnailPlaceholder() {
  Text(
      text = "Imagen",
      modifier = Modifier.clearAndSetSemantics {},
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}
