package io.github.themonstersp4.mejengueros.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
            onCourtSelected = {},
            onOpenCreateComplex = {},
        )
      }
    }

    composeRule.waitForIdle()
  }

  @Test
  fun catalogHeaderWithSecondaryCreateComplexActionRendersWithoutCrashing() {
    composeRule.setContent {
      MejenguerosTheme {
        HomeScreen(
            state = CourtCatalogUiState(isLoading = false),
            contentPadding = PaddingValues(),
            onSearchQueryChange = {},
            onProvinceSelected = {},
            onCantonSelected = {},
            onRetryLoad = {},
            onCourtSelected = {},
            onOpenCreateComplex = {},
        )
      }
    }

    composeRule.waitForIdle()
  }
}
