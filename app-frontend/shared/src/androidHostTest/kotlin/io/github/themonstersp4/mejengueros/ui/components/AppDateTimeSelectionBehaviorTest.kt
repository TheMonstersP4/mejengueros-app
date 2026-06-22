package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDateTimeSelectionBehaviorTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun weekdayChipRowKeepsTrailingDayReachableOnCompactWidths() {
    var selectedDay: String? = null

    composeRule.setContent {
      MejenguerosTheme {
        MejenguerosWeekdayChipRow(
            days =
                listOf(
                    "Lunes",
                    "Martes",
                    "Miércoles",
                    "Jueves",
                    "Viernes",
                    "Sábado",
                    "Domingo",
                ),
            selectedDays = emptySet(),
            onDayClick = { selectedDay = it },
            modifier = Modifier.width(180.dp).testTag(WeekdayRowTag),
        )
      }
    }

    val rowBounds = composeRule.onNodeWithTag(WeekdayRowTag).getUnclippedBoundsInRoot()
    val trailingChip = composeRule.onNodeWithText("Domingo")
    val trailingChipBeforeScroll = trailingChip.getUnclippedBoundsInRoot()

    assertTrue(trailingChipBeforeScroll.right > rowBounds.right)

    composeRule.onNodeWithTag(WeekdayRowTag).performTouchInput { swipeLeft() }

    val trailingChipAfterScroll = trailingChip.getUnclippedBoundsInRoot()
    assertTrue(trailingChipAfterScroll.right <= rowBounds.right)

    trailingChip.performClick()

    composeRule.runOnIdle { assertEquals("Domingo", selectedDay) }
  }

  @Test
  fun dateChipRowKeepsTrailingDateReachableOnCompactWidths() {
    var selectedIndex = -1

    composeRule.setContent {
      MejenguerosTheme {
        MejenguerosDateChipRow(
            dates =
                listOf(
                    "Lun" to "24",
                    "Mar" to "25",
                    "Mié" to "26",
                    "Jue" to "27",
                    "Vie" to "28",
                    "Sáb" to "29",
                    "Dom" to "30",
                ),
            selectedIndex = -1,
            onDateSelected = { selectedIndex = it },
            modifier = Modifier.width(180.dp).testTag(DateRowTag),
        )
      }
    }

    val rowBounds = composeRule.onNodeWithTag(DateRowTag).getUnclippedBoundsInRoot()
    val trailingChip = composeRule.onNodeWithText("30")
    val trailingChipBeforeScroll = trailingChip.getUnclippedBoundsInRoot()

    assertTrue(trailingChipBeforeScroll.right > rowBounds.right)

    composeRule.onNodeWithTag(DateRowTag).performTouchInput { swipeLeft() }

    val trailingChipAfterScroll = trailingChip.getUnclippedBoundsInRoot()
    assertTrue(trailingChipAfterScroll.right <= rowBounds.right)

    trailingChip.performClick()

    composeRule.runOnIdle { assertEquals(6, selectedIndex) }
  }

  @Test
  fun weekdayChipRowDistributesWidthEvenlyWhenLayoutHasEnoughSpace() {
    composeRule.setContent {
      MejenguerosTheme {
        MejenguerosWeekdayChipRow(
            days = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do"),
            selectedDays = emptySet(),
            onDayClick = {},
            modifier = Modifier.width(520.dp).testTag(WeekdayRowTag),
        )
      }
    }

    val firstChipBounds = composeRule.onNodeWithText("Lu").getUnclippedBoundsInRoot()
    val lastChip = composeRule.onNodeWithText("Do")
    val lastChipBounds = lastChip.getUnclippedBoundsInRoot()

    assertTrue(
        abs(
            ((firstChipBounds.right - firstChipBounds.left) -
                    (lastChipBounds.right - lastChipBounds.left))
                .value
        ) < 1f
    )
  }

  @Test
  fun dateChipRowDistributesWidthEvenlyWhenLayoutHasEnoughSpace() {
    composeRule.setContent {
      MejenguerosTheme {
        MejenguerosDateChipRow(
            dates = listOf("Hoy" to "16", "Mar" to "17", "Mié" to "18", "Jue" to "19"),
            selectedIndex = -1,
            onDateSelected = {},
            modifier = Modifier.width(520.dp).testTag(DateRowTag),
        )
      }
    }

    val firstChipBounds = composeRule.onNodeWithText("16").getUnclippedBoundsInRoot()
    val lastChip = composeRule.onNodeWithText("19")
    val lastChipBounds = lastChip.getUnclippedBoundsInRoot()

    assertTrue(
        abs(
            ((firstChipBounds.right - firstChipBounds.left) -
                    (lastChipBounds.right - lastChipBounds.left))
                .value
        ) < 1f
    )
  }

  private companion object {
    const val WeekdayRowTag = "weekday_chip_row"
    const val DateRowTag = "date_chip_row"
  }
}
