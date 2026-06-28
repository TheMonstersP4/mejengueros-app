package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.navigation.OwnerCourtAvailabilityEntrypoint
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
            onConfigureAvailability = { _ -> },
        )
      }
    }

    composeRule.onNodeWithText("Todavía no tenés complejos creados.").assertExists()
    composeRule.onNodeWithText("Crear complejo y primera cancha").assertExists().performClick()

    composeRule.runOnIdle { assertEquals(1, createClicks) }
  }

  @Test
  fun loadedStateShowsCourtsAndAvailabilityButtonTaggedWithCourtId() {

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
                                            id = "court-configured-id",
                                            name = "Court A",
                                            status = "ACTIVE",
                                            availabilityStatus =
                                                CourtAvailabilitySetupStatus.CONFIGURED,
                                        ),
                                        MyComplexHubCourt(
                                            id = "court-pending-id",
                                            name = "Court B",
                                            status = "ACTIVE",
                                            availabilityStatus =
                                                CourtAvailabilitySetupStatus.PENDING,
                                        ),
                                    ),
                            )
                        )
                ),
            username = "Owner",
            contentPadding = PaddingValues(),
            onCreateComplex = {},
            onRetry = {},
            onConfigureAvailability = { _ -> },
        )
      }
    }

    composeRule.onNodeWithText("North Sports Center").assertExists()
    composeRule.onNodeWithText("Disponibilidad configurada").assertExists()
    composeRule.onNodeWithText("Disponibilidad pendiente").assertExists()
    composeRule
        .onNodeWithTag("my_complex_configure_availability_button_court-pending-id")
        .assertExists()
  }

  @Test
  fun configureAvailabilityClickUsesCourtEntrypointWithRealCourtId() {
    var selectedEntrypoint: OwnerCourtAvailabilityEntrypoint? = null

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
                                provinceId = null,
                                cantonId = null,
                                latitude = null,
                                longitude = null,
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
            onConfigureAvailability = { selectedEntrypoint = it },
        )
      }
    }

    composeRule
        .onNodeWithTag("my_complex_root")
        .performScrollToNode(
            hasTestTag("my_complex_configure_availability_button_court-pending-id")
        )
    composeRule
        .onNodeWithTag("my_complex_configure_availability_button_court-pending-id")
        .performClick()

    composeRule.runOnIdle {
      assertEquals(
          OwnerCourtAvailabilityEntrypoint(
              courtId = "court-pending-id",
              courtName = "Court B",
              complexName = "North Sports Center",
          ),
          selectedEntrypoint,
      )
    }
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
            onConfigureAvailability = { _ -> },
        )
      }
    }

    composeRule.onNodeWithText("No pudimos cargar tu hub.").assertExists()
    composeRule.onNodeWithText("Reintentar").assertExists().performClick()

    composeRule.runOnIdle { assertEquals(1, retryClicks) }
  }
}
