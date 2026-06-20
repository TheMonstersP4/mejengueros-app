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
import io.github.themonstersp4.mejengueros.generated.resources.Res
import io.github.themonstersp4.mejengueros.generated.resources.anton_regular
import io.github.themonstersp4.mejengueros.generated.resources.archivo_narrow_bold
import io.github.themonstersp4.mejengueros.generated.resources.archivo_narrow_medium
import io.github.themonstersp4.mejengueros.generated.resources.archivo_narrow_regular
import io.github.themonstersp4.mejengueros.generated.resources.archivo_narrow_semi_bold
import org.jetbrains.compose.resources.Font

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

internal fun mejenguerosTypography(
    bodyFontFamily: FontFamily,
    displayFontFamily: FontFamily,
): Typography =
    Typography(
        displayLarge =
            goalstrykeTextStyle(
                fontFamily = displayFontFamily,
                fontSize = 48,
                lineHeight = 50,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.24f,
            ),
        displayMedium =
            goalstrykeTextStyle(
                fontFamily = displayFontFamily,
                fontSize = 38,
                lineHeight = 41,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.19f,
            ),
        displaySmall =
            goalstrykeTextStyle(
                fontFamily = displayFontFamily,
                fontSize = 30,
                lineHeight = 33,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.15f,
            ),
        headlineLarge =
            goalstrykeTextStyle(
                fontFamily = displayFontFamily,
                fontSize = 30,
                lineHeight = 34,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.15f,
            ),
        headlineMedium =
            goalstrykeTextStyle(
                fontFamily = displayFontFamily,
                fontSize = 26,
                lineHeight = 30,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.13f,
            ),
        headlineSmall =
            goalstrykeTextStyle(
                fontFamily = displayFontFamily,
                fontSize = 23,
                lineHeight = 27,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.12f,
            ),
        titleLarge =
            goalstrykeTextStyle(
                fontFamily = displayFontFamily,
                fontSize = 20,
                lineHeight = 24,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.10f,
            ),
        titleMedium =
            goalstrykeTextStyle(
                fontFamily = displayFontFamily,
                fontSize = 16,
                lineHeight = 20,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.08f,
            ),
        titleSmall =
            goalstrykeTextStyle(
                fontFamily = bodyFontFamily,
                fontSize = 14,
                lineHeight = 18,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = -0.14f,
            ),
        bodyLarge =
            goalstrykeTextStyle(
                fontFamily = bodyFontFamily,
                fontSize = 15,
                lineHeight = 23,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.15f,
            ),
        bodyMedium =
            goalstrykeTextStyle(
                fontFamily = bodyFontFamily,
                fontSize = 14,
                lineHeight = 22,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.14f,
            ),
        bodySmall =
            goalstrykeTextStyle(
                fontFamily = bodyFontFamily,
                fontSize = 12,
                lineHeight = 17,
                fontWeight = FontWeight.Normal,
                letterSpacing = -0.12f,
            ),
        labelLarge =
            goalstrykeTextStyle(
                fontFamily = bodyFontFamily,
                fontSize = 14,
                lineHeight = 18,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = -0.14f,
            ),
        labelMedium =
            goalstrykeTextStyle(
                fontFamily = bodyFontFamily,
                fontSize = 12,
                lineHeight = 12,
                fontWeight = FontWeight.SemiBold,
            ),
        labelSmall =
            goalstrykeTextStyle(
                fontFamily = bodyFontFamily,
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
  val typography =
      mejenguerosTypography(
          bodyFontFamily = bodyFontFamily(),
          displayFontFamily = displayFontFamily(),
      )

  MaterialTheme(
      colorScheme = GoalstrykeColorScheme,
      typography = typography,
      shapes = MejenguerosShapes,
      content = content,
  )
}

@Composable
private fun bodyFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.archivo_narrow_regular, weight = FontWeight.Normal),
        Font(Res.font.archivo_narrow_medium, weight = FontWeight.Medium),
        Font(Res.font.archivo_narrow_semi_bold, weight = FontWeight.SemiBold),
        Font(Res.font.archivo_narrow_bold, weight = FontWeight.Bold),
    )

@Composable
private fun displayFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.anton_regular, weight = FontWeight.Normal),
    )

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
