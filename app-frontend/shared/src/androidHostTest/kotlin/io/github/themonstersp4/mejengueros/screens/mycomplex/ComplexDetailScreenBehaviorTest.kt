package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.navigation.OwnerCourtAvailabilityEntrypoint
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComplexDetailScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun detailShowsAddCourtCallToActionAndPassesSelectedComplex() {
    var selectedComplexId: String? = null
    var selectedComplexName: String? = null

    composeRule.setContent {
      MejenguerosTheme {
        ComplexDetailScreen(
            complex = defaultComplex(),
            isLoading = false,
            errorMessage = null,
            contentPadding = PaddingValues(),
            onRetry = {},
            onAddCourt = { complexId, complexName ->
              selectedComplexId = complexId
              selectedComplexName = complexName
            },
            onConfigureAvailability = {},
        )
      }
    }

    composeRule
        .onNodeWithTag("complex_detail_root")
        .performScrollToNode(hasTestTag("complex_detail_add_court_button_complex-id"))
    composeRule.onNodeWithTag("complex_detail_add_court_button_complex-id").performClick()

    composeRule.runOnIdle {
      assertEquals("complex-id", selectedComplexId)
      assertEquals("North Sports Center", selectedComplexName)
    }
  }

  @Test
  fun configureAvailabilityClickUsesRealCourtId() {
    var selectedEntrypoint: OwnerCourtAvailabilityEntrypoint? = null

    composeRule.setContent {
      MejenguerosTheme {
        ComplexDetailScreen(
            complex = defaultComplex(),
            isLoading = false,
            errorMessage = null,
            contentPadding = PaddingValues(),
            onRetry = {},
            onAddCourt = { _, _ -> },
            onConfigureAvailability = { selectedEntrypoint = it },
        )
      }
    }

    composeRule
        .onNodeWithTag("complex_detail_root")
        .performScrollToNode(hasTestTag("my_complex_court_row_court-pending-id"))
    composeRule.onNodeWithTag("my_complex_court_row_court-pending-id").performClick()

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

  private fun defaultComplex() =
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
                      availabilityStatus = CourtAvailabilitySetupStatus.CONFIGURED,
                  ),
                  MyComplexHubCourt(
                      id = "court-pending-id",
                      name = "Court B",
                      status = "ACTIVE",
                      availabilityStatus = CourtAvailabilitySetupStatus.PENDING,
                  ),
              ),
      )
}
