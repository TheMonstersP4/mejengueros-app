package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.navigation.OwnerCourtAvailabilityEntrypoint
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComplexDetailScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun detailDoesNotShowAddCourtCallToActionInScrollableContent() {
    composeRule.setContent {
      MejenguerosTheme {
        ComplexDetailScreen(
            complex = defaultComplex(),
            isLoading = false,
            errorMessage = null,
            contentPadding = PaddingValues(),
            onRetry = {},
            onConfigureAvailability = {},
        )
      }
    }

    composeRule.onNodeWithText("123 Main Street").assertExists()
    composeRule.onNodeWithContentDescription("Agregar cancha").assertDoesNotExist()
    composeRule.onNodeWithTag("complex_detail_add_court_button_complex-id").assertDoesNotExist()
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
            onConfigureAvailability = { selectedEntrypoint = it },
            modifier = Modifier.width(280.dp),
        )
      }
    }

    composeRule
        .onNodeWithTag("complex_detail_root")
        .performScrollToNode(hasTestTag("my_complex_court_row_court-pending-id"))
    composeRule
        .onNodeWithTag("my_complex_court_icon_court-pending-id", useUnmergedTree = true)
        .assertExists()
    composeRule
        .onNodeWithText("Court B with an intentionally long configuration name that must wrap")
        .assertExists()
    composeRule
        .onNodeWithTag("my_complex_court_trailing_court-pending-id", useUnmergedTree = true)
        .assertExists()
    composeRule.onNodeWithText("Pendiente").assertExists()

    val courtsGroupBounds =
        composeRule
            .onNodeWithTag("my_complex_courts_group", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    val dividerBounds =
        composeRule
            .onNodeWithTag("my_complex_court_divider_court-configured-id", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    assertHorizontalEdgesMatch(courtsGroupBounds, dividerBounds)

    val headlineBounds =
        composeRule
            .onNodeWithTag("my_complex_court_headline_court-pending-id", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    val trailingBounds =
        composeRule
            .onNodeWithTag("my_complex_court_trailing_court-pending-id", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    assertTrue(
        headlineBounds.right < trailingBounds.left,
        "Expected trailing court actions to occupy their own horizontal slot.",
    )
    composeRule.onNodeWithTag("my_complex_court_row_court-pending-id").performClick()

    composeRule.runOnIdle {
      assertEquals(
          OwnerCourtAvailabilityEntrypoint(
              courtId = "court-pending-id",
              courtName = "Court B with an intentionally long configuration name that must wrap",
              complexName = "North Sports Center",
          ),
          selectedEntrypoint,
      )
    }
  }

  @Test
  fun activitySectionPlaceholderRowsAreVisible() {
    composeRule.setContent {
      MejenguerosTheme {
        ComplexDetailScreen(
            complex = defaultComplex(),
            isLoading = false,
            errorMessage = null,
            contentPadding = PaddingValues(),
            onRetry = {},
            onConfigureAvailability = {},
        )
      }
    }

    composeRule
        .onNodeWithTag("complex_detail_root")
        .performScrollToNode(hasTestTag("activity_resenas_row"))
    composeRule.onNodeWithTag("activity_resenas_row").assertExists()
    composeRule.onNodeWithText("Reseñas recibidas").assertExists()
    composeRule
        .onNodeWithTag("complex_detail_root")
        .performScrollToNode(hasTestTag("activity_reservas_row"))
    composeRule.onNodeWithTag("activity_reservas_row").assertExists()
    composeRule.onNodeWithText("Reservas de mis canchas").assertExists()
    composeRule.onAllNodesWithText("Próximamente")[0].assertExists()
  }

  @Test
  fun courtStatusPillShowsActivaForConfiguredAndPendienteForPending() {
    composeRule.setContent {
      MejenguerosTheme {
        ComplexDetailScreen(
            complex = defaultComplex(),
            isLoading = false,
            errorMessage = null,
            contentPadding = PaddingValues(),
            onRetry = {},
            onConfigureAvailability = {},
            modifier = Modifier.width(280.dp),
        )
      }
    }

    composeRule
        .onNodeWithTag("complex_detail_root")
        .performScrollToNode(hasTestTag("my_complex_courts_group"))
    composeRule.onAllNodesWithText("Activa")[0].assertExists()
    composeRule
        .onNodeWithTag("complex_detail_root")
        .performScrollToNode(hasTestTag("my_complex_court_row_court-pending-id"))
    composeRule.onAllNodesWithText("Pendiente")[0].assertExists()
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
                      name = "Court B with an intentionally long configuration name that must wrap",
                      status = "ACTIVE",
                      availabilityStatus = CourtAvailabilitySetupStatus.PENDING,
                  ),
              ),
      )

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
