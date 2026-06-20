package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun MejenguerosAuthHeadingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
  MejenguerosText(
      text = text,
      modifier = modifier,
      color = color,
      textAlign = textAlign,
      maxLines = maxLines,
      overflow = overflow,
      style = MaterialTheme.typography.displaySmall,
  )
}

@Composable
fun MejenguerosAuthTaglineText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
  MejenguerosText(
      text = text,
      modifier = modifier,
      color = color,
      textAlign = textAlign,
      maxLines = maxLines,
      overflow = overflow,
      style = MaterialTheme.typography.bodyLarge,
  )
}

@Composable
fun MejenguerosSupportingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
  MejenguerosText(
      text = text,
      modifier = modifier,
      color = color,
      textAlign = textAlign,
      maxLines = maxLines,
      overflow = overflow,
      style = MaterialTheme.typography.bodySmall,
  )
}

@Composable
fun MejenguerosErrorText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.error,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
  MejenguerosText(
      text = text,
      modifier = modifier,
      color = color,
      textAlign = textAlign,
      maxLines = maxLines,
      overflow = overflow,
      style = MaterialTheme.typography.bodyMedium,
  )
}

@Composable
private fun MejenguerosText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
      text = text,
      modifier = modifier,
      color = color,
      textAlign = textAlign,
      maxLines = maxLines,
      overflow = overflow,
      style = style,
  )
}
