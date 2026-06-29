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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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

data class MejenguerosListItemText(
    val title: String,
    val supportingText: String? = null,
    val headlineMaxLines: Int = 1,
    val headlineOverflow: TextOverflow = TextOverflow.Ellipsis,
    val supportingMaxLines: Int = 2,
    val supportingOverflow: TextOverflow = TextOverflow.Ellipsis,
    val headlineModifier: Modifier = Modifier,
    val supportingModifier: Modifier = Modifier,
)

data class MejenguerosListItemCustomContent(
    val headlineContent: @Composable () -> Unit,
    val supportingContent: (@Composable () -> Unit)? = null,
)

data class MejenguerosListItemStyle(
    val outerPadding: PaddingValues = PaddingValues(0.dp),
    val showDivider: Boolean = false,
    val shape: Shape = RectangleShape,
    val colors: ListItemColors? = null,
    val dividerModifier: Modifier = Modifier,
)

@Composable
fun MejenguerosListItem(
    text: MejenguerosListItemText,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    style: MejenguerosListItemStyle = MejenguerosListItemStyle(),
) {
  val headlineContent: @Composable () -> Unit = {
    Text(
        text = text.title,
        modifier = text.headlineModifier,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        maxLines = text.headlineMaxLines,
        overflow = text.headlineOverflow,
    )
  }
  val supportingContent =
      if (text.supportingText.isNullOrBlank()) {
        null
      } else {
        @Composable {
          Text(
              text = text.supportingText,
              modifier = text.supportingModifier,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = text.supportingMaxLines,
              overflow = text.supportingOverflow,
          )
        }
      }

  MejenguerosListItem(
      headlineContent = headlineContent,
      supportingContent = supportingContent,
      modifier = modifier,
      leading = leading,
      trailing = trailing,
      onClick = onClick,
      enabled = enabled,
      style = style,
  )
}

@Composable
fun MejenguerosListItem(
    content: MejenguerosListItemCustomContent,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    style: MejenguerosListItemStyle = MejenguerosListItemStyle(),
) {
  MejenguerosListItem(
      headlineContent = content.headlineContent,
      supportingContent = content.supportingContent,
      modifier = modifier,
      leading = leading,
      trailing = trailing,
      onClick = onClick,
      enabled = enabled,
      style = style,
  )
}

@Composable
private fun MejenguerosListItem(
    headlineContent: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)?,
    modifier: Modifier,
    leading: (@Composable RowScope.() -> Unit)?,
    trailing: (@Composable RowScope.() -> Unit)?,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    style: MejenguerosListItemStyle,
) {
  val itemShape = style.shape
  val itemColors = style.colors ?: ListItemDefaults.colors(containerColor = Color.Transparent)

  Column(modifier = modifier) {
    val leadingContent =
        if (leading == null) {
          null
        } else {
          @Composable { Row(content = leading) }
        }
    val trailingContent =
        if (trailing == null) {
          null
        } else {
          @Composable { Row(content = trailing) }
        }
    val listItemContent: @Composable () -> Unit = {
      ListItem(
          headlineContent = headlineContent,
          supportingContent = supportingContent,
          leadingContent = leadingContent,
          trailingContent = trailingContent,
          modifier = Modifier.fillMaxWidth().padding(style.outerPadding),
          colors = itemColors,
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
      )
    }

    if (onClick != null) {
      Surface(
          onClick = onClick,
          enabled = enabled,
          shape = itemShape,
          color = Color.Transparent,
          contentColor = MaterialTheme.colorScheme.onSurface,
      ) {
        listItemContent()
      }
    } else {
      Surface(
          shape = itemShape,
          color = Color.Transparent,
          contentColor = MaterialTheme.colorScheme.onSurface,
      ) {
        listItemContent()
      }
    }

    if (style.showDivider) {
      HorizontalDivider(
          modifier = Modifier.fillMaxWidth().then(style.dividerModifier),
          color = MaterialTheme.colorScheme.outlineVariant,
      )
    }
  }
}

@Composable
fun MejenguerosListGroup(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = shape,
      color = containerColor,
      border = border,
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
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = MaterialTheme.colorScheme.onSurface,
          disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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
              container = MaterialTheme.colorScheme.surfaceContainerHigh,
              content = MaterialTheme.colorScheme.onSurface,
              border = MaterialTheme.colorScheme.outlineVariant,
          )
      MejenguerosStatusPillStyle.Subtle ->
          MejenguerosStatusPillColors(
              container = MaterialTheme.colorScheme.surfaceContainerHighest,
              content = MaterialTheme.colorScheme.onSurfaceVariant,
              border = MaterialTheme.colorScheme.outlineVariant,
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
