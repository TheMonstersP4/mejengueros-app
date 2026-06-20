package io.github.themonstersp4.mejengueros.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ThemeTypographyTest {
  @Test
  fun displayAndButtonStylesUseDisplayFontFamily() {
    val displayFontFamily = FontFamily.Serif
    val bodyFontFamily = FontFamily.SansSerif

    val typography =
        mejenguerosTypography(
            bodyFontFamily = bodyFontFamily,
            displayFontFamily = displayFontFamily,
        )

    assertSame(displayFontFamily, typography.displayLarge.fontFamily)
    assertSame(displayFontFamily, typography.headlineMedium.fontFamily)
    assertSame(displayFontFamily, typography.titleMedium.fontFamily)
    assertEquals(FontWeight.Normal, typography.titleMedium.fontWeight)
  }

  @Test
  fun bodyAndLabelStylesUseBodyFontFamilyWithSemiboldLabels() {
    val displayFontFamily = FontFamily.Serif
    val bodyFontFamily = FontFamily.SansSerif

    val typography =
        mejenguerosTypography(
            bodyFontFamily = bodyFontFamily,
            displayFontFamily = displayFontFamily,
        )

    assertSame(bodyFontFamily, typography.bodyLarge.fontFamily)
    assertSame(bodyFontFamily, typography.labelLarge.fontFamily)
    assertSame(bodyFontFamily, typography.titleSmall.fontFamily)
    assertEquals(FontWeight.SemiBold, typography.labelLarge.fontWeight)
    assertEquals(FontWeight.SemiBold, typography.titleSmall.fontWeight)
  }
}
