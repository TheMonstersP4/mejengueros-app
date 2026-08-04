package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState
import io.github.themonstersp4.mejengueros.presentation.favorites.FavoriteCourtsUiState
import io.github.themonstersp4.mejengueros.presentation.profile.PlayerProfileUiState
import io.github.themonstersp4.mejengueros.screens.favorites.FavoriteCourtsScreen
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerProfileNavigationIntegrationTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun selectingProfileTabRendersAuthenticatedIdentityThroughNavDisplay() {
    val navigationState = testNavigationState()

    composeRule.setContent {
      MejenguerosTheme {
        PlayerProfileNavigationTestHost(
            navigationState = navigationState,
            displayName = "Marta Player",
            email = "marta.player@example.com",
        )
      }
    }

    composeRule.onNodeWithText("Mi perfil").performClick()

    composeRule.onNodeWithText("Marta Player").assertExists()
    composeRule.onNodeWithText("marta.player@example.com").assertExists()
    composeRule.onNodeWithContentDescription("Avatar de Marta Player").assertExists()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.Profile, navigationState.selectedRoute)
      assertEquals(listOf(PlayerProfileRoute), navigationState.currentBackStack.toList())
    }
  }

  @Test
  fun profileFavoriteEntryOpensCourtDetailAndBackReturnsToFavorites() {
    val navigationState = testNavigationState().apply { selectProfile() }
    composeRule.setContent {
      MejenguerosTheme {
        PlayerProfileNavigationTestHost(
            navigationState,
            "Marta Player",
            "marta.player@example.com",
        )
      }
    }
    composeRule.onNodeWithText("Mis canchas favoritas").performClick()
    composeRule.onNodeWithTag("favorite_court_card_court-id").performClick()
    composeRule.onNodeWithText("detail:court-id:Mejengas CR").assertExists()
    composeRule.runOnIdle {
      assertEquals(
          listOf(
              PlayerProfileRoute,
              FavoriteCourtsRoute,
              FavoriteCourtDetailRoute(
                  "court-id",
                  "complex-id",
                  "Mejengas CR",
                  "Cancha 1",
                  provinceName = "San José",
                  cantonName = "San José",
              ),
          ),
          navigationState.currentBackStack.toList(),
      )
      navigationState.closeCurrentDetail()
    }
    composeRule.onNodeWithTag("favorite_court_card_court-id").assertExists()
  }

  @Test
  fun profileRefreshDismissalFlowsThroughEntryContent() {
    val navigationState = testNavigationState().apply { selectProfile() }
    var dismissRequests = 0
    composeRule.setContent {
      MejenguerosTheme {
        PlayerProfileNavigationTestHost(
            navigationState = navigationState,
            displayName = "Marta Player",
            email = "marta.player@example.com",
            profileRefreshFailed = true,
            onDismissProfileRefreshFailure = { dismissRequests += 1 },
        )
      }
    }

    composeRule.onNodeWithText("Reintentar").assertDoesNotExist()
    composeRule.onNodeWithText("Cerrar").performClick()

    composeRule.runOnIdle { assertEquals(1, dismissRequests) }
  }

  @Composable
  private fun PlayerProfileNavigationTestHost(
      navigationState: AuthenticatedNavigationState,
      displayName: String,
      email: String,
      profileRefreshFailed: Boolean = false,
      onDismissProfileRefreshFailure: () -> Unit = {},
  ) {
    val shellActions =
        AuthenticatedShellActions(
            selectSearch = navigationState::selectSearch,
            selectReservations = navigationState::selectReservations,
            selectNotifications = navigationState::selectNotifications,
            selectProfile = navigationState::selectProfile,
            selectMyComplex = navigationState::selectMyComplex,
            returnToSearchRoot = navigationState::returnToSearchRoot,
            returnToMyComplexRoot = navigationState::returnToMyComplexRoot,
            openCatalogCourtDetail = navigationState::openCatalogCourtDetail,
            openCatalogReservation = navigationState::openCatalogReservation,
            openComplexDetail = navigationState::openComplexDetail,
            openAddCourt = navigationState::openAddCourt,
            openCreateComplex = navigationState::openCreateComplex,
            openCourtAvailability = navigationState::openCourtAvailability,
            closeAddCourtAfterSuccess = navigationState::closeAddCourtAfterSuccess,
            closeCurrentDetail = navigationState::closeCurrentDetail,
            signOut = {},
            refreshOwnerRole = {},
            isOwner = false,
            viewingAsPlayer = true,
        )

    NavDisplay(
        backStack = navigationState.currentBackStack,
        onBack = navigationState::closeCurrentDetail,
        entryProvider =
            entryProvider {
              entry<SearchRoute> {
                SearchCatalogEntryContent(
                    state = CourtCatalogUiState(isLoading = false),
                    shellActions = shellActions,
                    onSearchQueryChange = {},
                    onProvinceSelected = {},
                    onCantonSelected = {},
                    onRetryLoad = {},
                )
              }
              entry<PlayerProfileRoute> {
                PlayerProfileEntryContent(
                    state =
                        PlayerProfileUiState(
                            userId = "player-id",
                            fallbackDisplayName = displayName,
                            fallbackEmail = email,
                            profileRefreshFailed = profileRefreshFailed,
                        ),
                    shellActions = shellActions,
                    onDismissProfileRefreshFailure = onDismissProfileRefreshFailure,
                    onFavoriteCourtsClick = navigationState::openFavoriteCourts,
                )
              }
              entry<FavoriteCourtsRoute> {
                FavoriteCourtsScreen(
                    state =
                        FavoriteCourtsUiState(isLoading = false, courts = listOf(sampleCourt())),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    onOpenCourt = { court ->
                      navigationState.openFavoriteCourtDetail(
                          FavoriteCourtDetailRoute(
                              court.id,
                              court.complexId,
                              court.complexName,
                              court.courtName,
                              court.provinceName,
                              court.cantonName,
                              court.latitude,
                              court.longitude,
                              court.services,
                              court.ratingAverage,
                              court.ratingCount,
                              court.imageUrl,
                          )
                      )
                    },
                    onRemoveCourt = {},
                    onRetry = {},
                    onExploreCourts = navigationState::selectSearch,
                )
              }
              entry<FavoriteCourtDetailRoute> { route ->
                Text("detail:${route.courtId}:${route.complexName}")
              }
            },
    )
  }

  private fun testNavigationState(): AuthenticatedNavigationState =
      AuthenticatedNavigationState(
          selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Search),
          searchBackStack = NavBackStack<NavKey>(SearchRoute),
          reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
          notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
          profileBackStack = NavBackStack<NavKey>(PlayerProfileRoute),
          myComplexBackStack = NavBackStack<NavKey>(MyComplexRoute),
          ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
          myComplexHubReloadRequestKeyState = mutableStateOf(0),
          catalogReloadRequestKeyState = mutableStateOf(0),
          catalogCourtDetailReloadRequestKeyState = mutableStateOf(0),
          reservationsReloadRequestKeyState = mutableStateOf(0),
          viewingAsPlayerState = mutableStateOf(true),
          hydratedOwnerPreferenceUserIdState = mutableStateOf(null),
      )

  private fun sampleCourt() =
      io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem(
          "court-id",
          "complex-id",
          "Mejengas CR",
          "Cancha 1",
          "province-id",
          "San José",
          "canton-id",
          "San José",
          services = emptyList(),
          ratingAverage = null,
          ratingCount = 0,
          imageUrl = null,
          isReservableToday = false,
      )
}
