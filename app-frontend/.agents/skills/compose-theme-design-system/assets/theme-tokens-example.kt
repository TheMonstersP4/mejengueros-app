package example.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme =
    lightColorScheme(
        primary = AppLightPrimary,
        onPrimary = AppLightOnPrimary,
        background = AppLightBackground,
        onBackground = AppLightOnBackground,
        surface = AppLightSurface,
        onSurface = AppLightOnSurface,
        error = AppLightError,
    )

private val DarkColors: ColorScheme =
    darkColorScheme(
        primary = AppDarkPrimary,
        onPrimary = AppDarkOnPrimary,
        background = AppDarkBackground,
        onBackground = AppDarkOnBackground,
        surface = AppDarkSurface,
        onSurface = AppDarkOnSurface,
        error = AppDarkError,
    )

@Immutable
data class AppStatusColors(
    val success: Color,
    val failure: Color,
    val unknown: Color,
)

private val LocalAppStatusColors =
    staticCompositionLocalOf {
        AppStatusColors(
            success = Color.Unspecified,
            failure = Color.Unspecified,
            unknown = Color.Unspecified,
        )
    }

object AppTheme {
    val status: AppStatusColors
        @Composable get() = LocalAppStatusColors.current
}

@Composable
fun AppTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    val statusColors =
        AppStatusColors(
            success = Color(0xFF1F883D),
            failure = colorScheme.error,
            unknown = colorScheme.onSurfaceVariant,
        )

    CompositionLocalProvider(LocalAppStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
