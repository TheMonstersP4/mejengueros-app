package io.github.themonstersp4.mejengueros.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColorScheme =
    lightColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,
        secondary = Secondary,
        onSecondary = OnSecondary,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,
        error = Error,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        surfaceContainer = SurfaceContainer,
        surfaceContainerLow = SurfaceContainerLow,
        surfaceContainerHighest = SurfaceContainerHighest,
        onSurface = OnSurface,
        onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
        outlineVariant = OutlineVariant,
    )

private val MejenguerosTypography =
    Typography(
        headlineLarge =
            appleTextStyle(fontSize = 34, lineHeight = 38, fontWeight = FontWeight.SemiBold),
        headlineMedium =
            appleTextStyle(fontSize = 28, lineHeight = 32, fontWeight = FontWeight.SemiBold),
        headlineSmall =
            appleTextStyle(fontSize = 24, lineHeight = 28, fontWeight = FontWeight.SemiBold),
        titleLarge =
            appleTextStyle(fontSize = 21, lineHeight = 25, fontWeight = FontWeight.SemiBold),
        titleMedium =
            appleTextStyle(fontSize = 17, lineHeight = 21, fontWeight = FontWeight.SemiBold),
        titleSmall =
            appleTextStyle(fontSize = 14, lineHeight = 18, fontWeight = FontWeight.SemiBold),
        bodyLarge = appleTextStyle(fontSize = 17, lineHeight = 25, fontWeight = FontWeight.Normal),
        bodyMedium = appleTextStyle(fontSize = 14, lineHeight = 20, fontWeight = FontWeight.Normal),
        bodySmall = appleTextStyle(fontSize = 12, lineHeight = 16, fontWeight = FontWeight.Normal),
        labelLarge =
            appleTextStyle(fontSize = 14, lineHeight = 18, fontWeight = FontWeight.SemiBold),
        labelMedium =
            appleTextStyle(fontSize = 12, lineHeight = 12, fontWeight = FontWeight.SemiBold),
        labelSmall =
            appleTextStyle(fontSize = 11, lineHeight = 11, fontWeight = FontWeight.SemiBold),
    )

private val MejenguerosShapes =
    Shapes(
        extraSmall = RoundedCornerShape(5.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(11.dp),
        large = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(18.dp),
    )

@Composable
fun MejenguerosTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme = LightColorScheme,
      typography = MejenguerosTypography,
      shapes = MejenguerosShapes,
      content = content,
  )
}

private fun appleTextStyle(
    fontSize: Int,
    lineHeight: Int,
    fontWeight: FontWeight,
): TextStyle =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = fontWeight,
        letterSpacing = (-0.2).sp,
    )
