package io.github.themonstersp4.mejengueros.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GoalstrykeColorScheme =
    darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,
        secondary = Secondary,
        onSecondary = OnSecondary,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,
        tertiary = Tertiary,
        onTertiary = OnTertiary,
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = OnTertiaryContainer,
        error = Error,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        surfaceDim = SurfaceDim,
        surfaceBright = SurfaceBright,
        surfaceContainerLowest = SurfaceContainerLowest,
        surfaceContainerLow = SurfaceContainerLow,
        surfaceContainer = SurfaceContainer,
        surfaceContainerHigh = SurfaceContainerHigh,
        surfaceContainerHighest = SurfaceContainerHighest,
        surfaceVariant = SurfaceVariant,
        onSurface = OnSurface,
        onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
        outlineVariant = OutlineVariant,
        inverseSurface = InverseSurface,
        inverseOnSurface = InverseOnSurface,
        inversePrimary = InversePrimary,
    )

// GOALSTRYKE uses Anton for display/button text and Archivo Narrow for body text.
// Font files are not in the repository yet; KMP should load approved packaged font resources
// from commonMain/composeResources/font with org.jetbrains.compose.resources.Font.
private val BodyFontFamily = FontFamily.Default
private val DisplayFontFamily = FontFamily.Default

private val MejenguerosTypography =
    Typography(
        displayLarge =
            goalstrykeTextStyle(
                fontFamily = DisplayFontFamily,
                fontSize = 48,
                lineHeight = 50,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.24f,
            ),
        displayMedium =
            goalstrykeTextStyle(
                fontFamily = DisplayFontFamily,
                fontSize = 38,
                lineHeight = 41,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.19f,
            ),
        displaySmall =
            goalstrykeTextStyle(
                fontFamily = DisplayFontFamily,
                fontSize = 30,
                lineHeight = 33,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.15f,
            ),
        headlineLarge =
            goalstrykeTextStyle(
                fontFamily = DisplayFontFamily,
                fontSize = 30,
                lineHeight = 34,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.15f,
            ),
        headlineMedium =
            goalstrykeTextStyle(
                fontFamily = DisplayFontFamily,
                fontSize = 26,
                lineHeight = 30,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.13f,
            ),
        headlineSmall =
            goalstrykeTextStyle(
                fontFamily = DisplayFontFamily,
                fontSize = 23,
                lineHeight = 27,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.12f,
            ),
        titleLarge =
            goalstrykeTextStyle(
                fontFamily = DisplayFontFamily,
                fontSize = 20,
                lineHeight = 24,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.10f,
            ),
        titleMedium =
            goalstrykeTextStyle(
                fontFamily = DisplayFontFamily,
                fontSize = 16,
                lineHeight = 20,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.08f,
            ),
        titleSmall =
            goalstrykeTextStyle(
                fontFamily = BodyFontFamily,
                fontSize = 14,
                lineHeight = 18,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = -0.14f,
            ),
        bodyLarge =
            goalstrykeTextStyle(
                fontFamily = BodyFontFamily,
                fontSize = 15,
                lineHeight = 23,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.15f,
            ),
        bodyMedium =
            goalstrykeTextStyle(
                fontFamily = BodyFontFamily,
                fontSize = 14,
                lineHeight = 22,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.14f,
            ),
        bodySmall =
            goalstrykeTextStyle(
                fontFamily = BodyFontFamily,
                fontSize = 12,
                lineHeight = 17,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.12f,
            ),
        labelLarge =
            goalstrykeTextStyle(
                fontFamily = BodyFontFamily,
                fontSize = 14,
                lineHeight = 18,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = -0.14f,
            ),
        labelMedium =
            goalstrykeTextStyle(
                fontFamily = BodyFontFamily,
                fontSize = 12,
                lineHeight = 12,
                fontWeight = FontWeight.SemiBold,
            ),
        labelSmall =
            goalstrykeTextStyle(
                fontFamily = BodyFontFamily,
                fontSize = 11,
                lineHeight = 11,
                fontWeight = FontWeight.SemiBold,
            ),
    )

private val MejenguerosShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )

@Composable
fun MejenguerosTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme = GoalstrykeColorScheme,
      typography = MejenguerosTypography,
      shapes = MejenguerosShapes,
      content = content,
  )
}

private fun goalstrykeTextStyle(
    fontFamily: FontFamily,
    fontSize: Int,
    lineHeight: Int,
    fontWeight: FontWeight,
    letterSpacing: Float = 0f,
): TextStyle =
    TextStyle(
        fontFamily = fontFamily,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing.sp,
    )
