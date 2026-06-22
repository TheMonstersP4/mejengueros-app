package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val ProviderIconStrokeWidth = 2.dp
private const val GoogleProviderArcStartAngle = 42f
private const val GoogleProviderArcSweepAngle = 276f
private const val GoogleProviderLineStartFraction = 0.55f
private const val MicrosoftProviderGapFraction = 0.12f

private val VisibilityImageVector: ImageVector =
    ImageVector.Builder(
            name = "Visibility",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        .path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
          moveTo(2.5f, 12f)
          curveTo(5.5f, 7f, 8.5f, 5f, 12f, 5f)
          curveTo(15.5f, 5f, 18.5f, 7f, 21.5f, 12f)
          curveTo(18.5f, 17f, 15.5f, 19f, 12f, 19f)
          curveTo(8.5f, 19f, 5.5f, 17f, 2.5f, 12f)
          close()
          moveTo(12f, 9f)
          curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
          curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
          curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
          curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
          close()
        }
        .build()

private val VisibilityOffImageVector: ImageVector =
    ImageVector.Builder(
            name = "VisibilityOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        .path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
          moveTo(3f, 3f)
          lineTo(21f, 21f)
          moveTo(10.6f, 10.6f)
          curveTo(10.23f, 10.98f, 10f, 11.48f, 10f, 12f)
          curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
          curveTo(12.52f, 14f, 13.02f, 13.77f, 13.4f, 13.4f)
          moveTo(7.1f, 7.1f)
          curveTo(5.42f, 8.15f, 3.92f, 9.8f, 2.5f, 12f)
          curveTo(5.5f, 17f, 8.5f, 19f, 12f, 19f)
          curveTo(13.55f, 19f, 15f, 18.61f, 16.35f, 17.83f)
          moveTo(10.2f, 5.25f)
          curveTo(10.78f, 5.08f, 11.38f, 5f, 12f, 5f)
          curveTo(15.5f, 5f, 18.5f, 7f, 21.5f, 12f)
          curveTo(20.72f, 13.3f, 19.93f, 14.38f, 19.1f, 15.25f)
        }
        .build()

@Composable
fun VisibilityIcon(
    visible: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
  Icon(
      imageVector = if (visible) VisibilityOffImageVector else VisibilityImageVector,
      contentDescription = contentDescription,
      modifier = modifier,
  )
}

@Composable
fun GoogleProviderIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
  val strokeWidth = with(LocalDensity.current) { ProviderIconStrokeWidth.toPx() }

  Canvas(modifier = modifier) {
    val inset = strokeWidth
    val diameter = size.minDimension - (inset * 2)
    drawArc(
        color = tint,
        startAngle = GoogleProviderArcStartAngle,
        sweepAngle = GoogleProviderArcSweepAngle,
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(diameter, diameter),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
    val centerY = size.height / 2f
    drawLine(
        color = tint,
        start =
            androidx.compose.ui.geometry.Offset(
                size.width * GoogleProviderLineStartFraction,
                centerY,
            ),
        end = androidx.compose.ui.geometry.Offset(size.width - inset, centerY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
  }
}

@Composable
fun MicrosoftProviderIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
  Canvas(modifier = modifier) {
    val gap = size.minDimension * MicrosoftProviderGapFraction
    val cell = (size.minDimension - gap) / 2f
    drawRect(
        color = tint,
        topLeft = androidx.compose.ui.geometry.Offset.Zero,
        size = androidx.compose.ui.geometry.Size(cell, cell),
    )
    drawRect(
        color = tint,
        topLeft = androidx.compose.ui.geometry.Offset(cell + gap, 0f),
        size = androidx.compose.ui.geometry.Size(cell, cell),
    )
    drawRect(
        color = tint,
        topLeft = androidx.compose.ui.geometry.Offset(0f, cell + gap),
        size = androidx.compose.ui.geometry.Size(cell, cell),
    )
    drawRect(
        color = tint,
        topLeft = androidx.compose.ui.geometry.Offset(cell + gap, cell + gap),
        size = androidx.compose.ui.geometry.Size(cell, cell),
    )
  }
}
