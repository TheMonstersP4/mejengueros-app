package io.github.themonstersp4.mejengueros.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun loadErrorStateRendersWithoutCrashing() {
    composeRule.setContent {
      MejenguerosTheme {
        HomeScreen(
            state = CourtCatalogUiState(isLoading = false, loadErrorMessage = "No disponible"),
            contentPadding = PaddingValues(),
            onSearchQueryChange = {},
            onProvinceSelected = {},
            onCantonSelected = {},
            onRetryLoad = {},
            onOpenCourtDetail = {},
        )
      }
    }

    composeRule.waitForIdle()
  }

  @Test
  fun playerCatalogDoesNotContainOwnerCta() {
    composeRule.setContent {
      MejenguerosTheme {
        HomeScreen(
            state = CourtCatalogUiState(isLoading = false),
            contentPadding = PaddingValues(),
            onSearchQueryChange = {},
            onProvinceSelected = {},
            onCantonSelected = {},
            onRetryLoad = {},
            onOpenCourtDetail = {},
        )
      }
    }

    composeRule.onNodeWithTag("catalog_create_complex_button").assertDoesNotExist()
    composeRule.onNodeWithText("¿Administrás un complejo?").assertDoesNotExist()
    composeRule.onNodeWithText("Crear complejo").assertDoesNotExist()
  }

  @Test
  fun homeScreenDoesNotExposeAvailabilityShortcut() {
    composeRule.setContent {
      MejenguerosTheme {
        HomeScreen(
            state = CourtCatalogUiState(isLoading = false),
            contentPadding = PaddingValues(),
            onSearchQueryChange = {},
            onProvinceSelected = {},
            onCantonSelected = {},
            onRetryLoad = {},
            onOpenCourtDetail = {},
        )
      }
    }

    composeRule.onNodeWithTag("home_owner_availability_button").assertDoesNotExist()
    composeRule.onNodeWithText("Última cancha creada").assertDoesNotExist()
  }

  @Test
  fun catalogCardsExposeNavigationClickAction() {
    composeRule.setContent {
      MejenguerosTheme {
        HomeScreen(
            state =
                CourtCatalogUiState(
                    isLoading = false,
                    visibleCourts =
                        listOf(
                            CourtCatalogItem(
                                id = "court-id",
                                complexId = "complex-id",
                                complexName = "Mejengas CR",
                                courtName = "Cancha 1",
                                provinceId = "sj",
                                provinceName = "San José",
                                cantonId = "central",
                                cantonName = "Central",
                                services = listOf("Parqueo"),
                                ratingAverage = 4.8,
                                ratingCount = 12,
                                imageUrl = null,
                                isReservableToday = true,
                            )
                        ),
                ),
            contentPadding = PaddingValues(),
            onSearchQueryChange = {},
            onProvinceSelected = {},
            onCantonSelected = {},
            onRetryLoad = {},
            onOpenCourtDetail = {},
        )
      }
    }

    composeRule
        .onNodeWithTag("catalog_court_card_court-id")
        .assertExists()
        .assert(SemanticsMatcher("has click action") { hasClickAction().matches(it) })
  }

  @Test
  fun catalogCardClickTriggersCatalogItemCallback() {
    var openedCourtId: String? = null

    composeRule.setContent {
      MejenguerosTheme {
        HomeScreen(
            state =
                CourtCatalogUiState(
                    isLoading = false,
                    visibleCourts =
                        listOf(
                            CourtCatalogItem(
                                id = "court-tap-id",
                                complexId = "complex-id",
                                complexName = "Mejengas CR",
                                courtName = "Cancha A",
                                provinceId = "sj",
                                provinceName = "San José",
                                cantonId = "central",
                                cantonName = "Central",
                                services = emptyList(),
                                ratingAverage = null,
                                ratingCount = 0,
                                imageUrl = null,
                                isReservableToday = false,
                            )
                        ),
                ),
            contentPadding = PaddingValues(),
            onSearchQueryChange = {},
            onProvinceSelected = {},
            onCantonSelected = {},
            onRetryLoad = {},
            onOpenCourtDetail = { court -> openedCourtId = court.id },
        )
      }
    }

    composeRule.onNodeWithTag("catalog_court_card_court-tap-id").performClick()

    composeRule.runOnIdle { kotlin.test.assertEquals("court-tap-id", openedCourtId) }
  }
}
