package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
class ComplexDetailRouteContentBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun successDialogShowsAndConfirmAcknowledgesAndClearsMessage() {
    var state by
        mutableStateOf(
            MyComplexUiState(
                complexes = listOf(defaultComplex()),
                courtImageSuccessMessage = "La imagen de la cancha se actualizó correctamente.",
            )
        )
    var acknowledgements by mutableIntStateOf(0)

    composeRule.setContent {
      MejenguerosTheme {
        ComplexDetailRouteContent(
            route = ComplexDetailRoute("complex-id"),
            state = state,
            shellActions = testShellActions(),
            onRetry = {},
            onAcknowledgeCourtImageSuccess = {
              acknowledgements += 1
              state = state.copy(courtImageSuccessMessage = null)
            },
        )
      }
    }

    composeRule.onNodeWithTag("complex_detail_image_success_dialog").assertExists()
    composeRule.onNodeWithText("Imagen actualizada").assertExists()
    composeRule.onNodeWithText("La imagen de la cancha se actualizó correctamente.").assertExists()

    composeRule.onNodeWithTag("mejengueros_confirmation_dialog_confirm").performClick()

    composeRule.runOnIdle { assertEquals(1, acknowledgements) }
    composeRule.onNodeWithTag("complex_detail_image_success_dialog").assertDoesNotExist()
  }

  private fun testShellActions() =
      AuthenticatedShellActions(
          selectSearch = {},
          selectReservations = {},
          selectNotifications = {},
          selectMyComplex = {},
          returnToSearchRoot = {},
          returnToMyComplexRoot = {},
          openCatalogCourtDetail = {},
          openCatalogReservation = {},
          openComplexDetail = {},
          openAddCourt = { _, _ -> },
          openCreateComplex = {},
          openCourtAvailability = {},
          closeAddCourtAfterSuccess = {},
          closeCurrentDetail = {},
          signOut = {},
          refreshOwnerRole = {},
          isOwner = true,
      )

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
                  )
              ),
      )
}
