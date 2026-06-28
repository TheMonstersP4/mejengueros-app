package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
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
            username = "Owner",
            contentPadding = PaddingValues(),
            onCreateComplex = {},
            onRetry = {},
            onOpenComplexDetail = {},
        )
      }
    }

    composeRule.onNodeWithTag("my_complex_loading_indicator").assertExists()
    composeRule.onNodeWithText("Cargando tu hub de complejos...").assertExists()
  }

  @Test
  fun emptyStateShowsCreateCallToAction() {
    var createClicks = 0

    composeRule.setContent {
      MejenguerosTheme {
        MyComplexScreen(
            state = MyComplexUiState(complexes = emptyList()),
            username = "Owner",
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

    composeRule.setContent {
      MejenguerosTheme {
        MyComplexScreen(
            state =
                MyComplexUiState(
                    complexes =
                        listOf(
                            MyComplexHubComplex(
                                id = "complex-id",
                                name = "North Sports Center",
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
                            )
                        )
                ),
            username = "Owner",
            contentPadding = PaddingValues(),
            onCreateComplex = {},
            onRetry = {},
            onOpenComplexDetail = { selectedComplexId = it },
        )
      }
    }

    composeRule.onNodeWithText("TUS COMPLEJOS").assertExists()
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
            username = "Owner",
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
}
