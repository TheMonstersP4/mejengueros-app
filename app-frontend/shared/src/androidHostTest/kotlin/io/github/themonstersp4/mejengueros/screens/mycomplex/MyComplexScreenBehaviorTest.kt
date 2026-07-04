package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MyComplexScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun loadingStateShowsIndicatorAndCopy() {
    composeRule.setContent {
      MejenguerosTheme {
        MyComplexScreen(
            state = MyComplexUiState(isLoading = true),
            contentPadding = PaddingValues(),
            onCreateComplex = {},
            onRetry = {},
            onOpenComplexDetail = {},
        )
      }
    }

    composeRule.onNodeWithTag("my_complex_loading", useUnmergedTree = true).assertExists()
    composeRule.onNodeWithTag("my_complex_loading_indicator").assertExists()
    composeRule.onNodeWithText("Cargando tu hub de complejos…").assertExists()
    composeRule.onNodeWithText("Tus complejos deportivos").assertExists()
    composeRule
        .onNodeWithText("Gestioná canchas, disponibilidad y reservas desde un solo lugar.")
        .assertExists()
    composeRule.onNodeWithText("Mi complejo").assertDoesNotExist()
  }

  @Test
  fun emptyStateShowsCreateCallToAction() {
    var createClicks = 0

    composeRule.setContent {
      MejenguerosTheme {
        MyComplexScreen(
            state = MyComplexUiState(complexes = emptyList()),
            contentPadding = PaddingValues(),
            onCreateComplex = { createClicks += 1 },
            onRetry = {},
            onOpenComplexDetail = {},
        )
      }
    }

    composeRule.onNodeWithText("Todavía no tenés complejos creados.").assertExists()
    composeRule.onNodeWithText("Crear complejo y primera cancha").assertExists().performClick()

    composeRule.runOnIdle { assertEquals(1, createClicks) }
  }

  @Test
  fun loadedStateShowsOwnerComplexListAndOpensSelectedDetail() {
    var selectedComplexId: String? = null
    val longComplexName =
        "North Sports Center with a very long owner-facing name that must keep wrapping"

    composeRule.setContent {
      MejenguerosTheme {
        MyComplexScreen(
            state =
                MyComplexUiState(
                    complexes =
                        listOf(
                            MyComplexHubComplex(
                                id = "complex-id",
                                name = longComplexName,
                                address = "123 Main Street",
                                provinceId = "province-id",
                                cantonId = "canton-id",
                                latitude = 9.935,
                                longitude = -84.091,
                                status = "ACTIVE",
                                courts =
                                    listOf(
                                        MyComplexHubCourt(
                                            id = "court-pending-id",
                                            name = "Court B",
                                            status = "ACTIVE",
                                            availabilityStatus =
                                                CourtAvailabilitySetupStatus.PENDING,
                                        )
                                    ),
                            ),
                            MyComplexHubComplex(
                                id = "complex-id-2",
                                name = "South Sports Center",
                                address = "456 Side Street",
                                provinceId = "province-id",
                                cantonId = "canton-id",
                                latitude = 9.9,
                                longitude = -84.1,
                                status = "ACTIVE",
                                courts = emptyList(),
                            ),
                        )
                ),
            contentPadding = PaddingValues(),
            onCreateComplex = {},
            onRetry = {},
            onOpenComplexDetail = { selectedComplexId = it },
            modifier = Modifier.width(280.dp),
        )
      }
    }

    composeRule.onNodeWithText(longComplexName).assertExists()
    composeRule.onNodeWithText("123 Main Street · 1 cancha").assertExists()
    composeRule
        .onNodeWithTag("my_complex_list_icon_complex-id", useUnmergedTree = true)
        .assertExists()
    composeRule
        .onNodeWithTag("my_complex_list_trailing_complex-id", useUnmergedTree = true)
        .assertExists()

    val listGroupBounds =
        composeRule.onNodeWithTag("my_complex_list_group").getUnclippedBoundsInRoot()
    val dividerBounds =
        composeRule
            .onNodeWithTag("my_complex_list_divider_complex-id", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    assertHorizontalEdgesMatch(listGroupBounds, dividerBounds)

    val longItemBounds =
        composeRule.onNodeWithTag("my_complex_list_item_complex-id").getUnclippedBoundsInRoot()
    val shortItemBounds =
        composeRule.onNodeWithTag("my_complex_list_item_complex-id-2").getUnclippedBoundsInRoot()
    assertTrue(
        (longItemBounds.bottom - longItemBounds.top) >
            (shortItemBounds.bottom - shortItemBounds.top),
        "Expected long-name row to be taller than the short-name row.",
    )

    val headlineBounds =
        composeRule
            .onNodeWithTag("my_complex_list_headline_complex-id", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    val trailingBounds =
        composeRule
            .onNodeWithTag("my_complex_list_trailing_complex-id", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    assertTrue(
        headlineBounds.right < trailingBounds.left,
        "Expected trailing affordance to occupy its own horizontal slot.",
    )
    composeRule.onNodeWithTag("my_complex_list_item_complex-id").assertExists().performClick()

    composeRule.runOnIdle { assertEquals("complex-id", selectedComplexId) }
  }

  @Test
  fun errorStateShowsRetryAction() {
    var retryClicks = 0

    composeRule.setContent {
      MejenguerosTheme {
        MyComplexScreen(
            state = MyComplexUiState(errorMessage = "No pudimos cargar tu hub."),
            contentPadding = PaddingValues(),
            onCreateComplex = {},
            onRetry = { retryClicks += 1 },
            onOpenComplexDetail = {},
        )
      }
    }

    composeRule.onNodeWithText("No pudimos cargar tu hub.").assertExists()
    composeRule.onNodeWithText("Reintentar").assertExists().performClick()

    composeRule.runOnIdle { assertEquals(1, retryClicks) }
  }

  private fun assertHorizontalEdgesMatch(
      containerBounds: DpRect,
      dividerBounds: DpRect,
      tolerance: Float = 1f,
  ) {
    assertTrue(
        abs(containerBounds.left.value - dividerBounds.left.value) <= tolerance,
        "Expected divider left=${dividerBounds.left} to match container left=${containerBounds.left} within $tolerance.",
    )
    assertTrue(
        abs(containerBounds.right.value - dividerBounds.right.value) <= tolerance,
        "Expected divider right=${dividerBounds.right} to match container right=${containerBounds.right} within $tolerance.",
    )
  }
}
