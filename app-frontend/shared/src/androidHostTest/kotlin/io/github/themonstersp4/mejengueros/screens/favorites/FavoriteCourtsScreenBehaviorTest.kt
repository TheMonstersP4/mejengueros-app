package io.github.themonstersp4.mejengueros.screens.favorites

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.presentation.favorites.FavoriteCourtsUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoriteCourtsScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun rendersLoadingEmptyErrorAndExplore() {
    var explored = false
    val state = setHost(FavoriteCourtsUiState(), onExplore = { explored = true })
    composeRule.onNodeWithTag("favorite_courts_loading").assertExists()
    state.value = FavoriteCourtsUiState(isLoading = false)
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Todavía no tenés canchas favoritas").assertExists()
    composeRule.onNodeWithTag("favorite_courts_explore").performClick()
    assertEquals(true, explored)
    state.value = FavoriteCourtsUiState(isLoading = false, errorMessage = "falló")
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("favorite_courts_retry").assertHasClickAction()
  }

  @Test
  fun cardAndRemoveAreSeparateAndRemovingIsDisabled() {
    var opened = false
    var removed = false
    val state =
        setHost(
            FavoriteCourtsUiState(isLoading = false, courts = listOf(court())),
            { opened = true },
            { removed = true },
        )
    composeRule.onNodeWithTag("favorite_court_card_a").performClick()
    composeRule.onNodeWithTag("favorite_court_remove_a").performClick()
    assertEquals(true, opened)
    assertEquals(true, removed)
    state.value =
        FavoriteCourtsUiState(
            isLoading = false,
            courts = listOf(court()),
            removingCourtIds = setOf("a"),
        )
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("favorite_court_remove_a").assertIsNotEnabled()
  }

  private fun setHost(
      state: FavoriteCourtsUiState,
      open: (CourtCatalogItem) -> Unit = {},
      remove: (String) -> Unit = {},
      onExplore: () -> Unit = {},
  ): MutableState<FavoriteCourtsUiState> {
    val uiState = mutableStateOf(state)
    composeRule.setContent {
      MejenguerosTheme {
        FavoriteCourtsScreen(uiState.value, PaddingValues(), open, remove, {}, onExplore)
      }
    }
    return uiState
  }

  private fun court() =
      CourtCatalogItem(
          "a",
          "c",
          "Complex",
          "Court",
          "p",
          "Province",
          "ct",
          "Canton",
          services = emptyList(),
          ratingAverage = null,
          ratingCount = 0,
          imageUrl = null,
          isReservableToday = false,
      )
}
