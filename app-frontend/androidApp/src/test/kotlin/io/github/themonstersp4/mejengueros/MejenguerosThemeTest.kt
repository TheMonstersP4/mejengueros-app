package io.github.themonstersp4.mejengueros

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MejenguerosThemeTest {

  @Test
  fun manifestAppliesMejenguerosThemeToApplication() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val applicationInfo =
        context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.ApplicationInfoFlags.of(0),
        )

    assertEquals(R.style.Theme_Mejengueros, applicationInfo.theme)
  }

  @Test
  fun appThemeOwnsDarkWindowBackground() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val themedContext = context.createPackageContext(context.packageName, 0)
    themedContext.setTheme(R.style.Theme_Mejengueros)

    val background = themedContext.resolveDrawableAttribute(android.R.attr.windowBackground)

    assertIs<ColorDrawable>(background)
    assertEquals(GoalstrykeWindowBackground, background.color)
  }

  @Test
  fun appThemeKeepsSystemBarsDark() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val themedContext = context.createPackageContext(context.packageName, 0)
    themedContext.setTheme(R.style.Theme_Mejengueros)

    assertEquals(
        GoalstrykeWindowBackground,
        themedContext.resolveColorAttribute(android.R.attr.statusBarColor),
    )
    assertEquals(
        GoalstrykeWindowBackground,
        themedContext.resolveColorAttribute(android.R.attr.navigationBarColor),
    )
  }

  private fun Context.resolveDrawableAttribute(attribute: Int) =
      obtainStyledAttributes(intArrayOf(attribute)).let { attributes ->
        try {
          attributes.getDrawable(0)
        } finally {
          attributes.recycle()
        }
      }

  private fun Context.resolveColorAttribute(attribute: Int): Int {
    val value = TypedValue()
    assertTrue(theme.resolveAttribute(attribute, value, true))
    return value.data
  }

  private companion object {
    val GoalstrykeWindowBackground = Color.rgb(0x11, 0x14, 0x15)
  }
}
