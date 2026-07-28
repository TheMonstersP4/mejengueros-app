package io.github.themonstersp4.mejengueros.screens.courtdetail

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.CourtReview
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailSlot
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailUiState
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtFavoriteStatus
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CourtDetailScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun detailScreenRendersHeaderRatingServiciosAndDisponibilidadSections() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = listOf("Parqueo", "Iluminación"),
            ratingAverage = 4.5,
            ratingCount = 8,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    isLoadingSlots = false,
                    availabilityHeadline = "Hoy · slots de 1 hora",
                    slots =
                        listOf(
                            CourtDetailSlot(displayTime = "08:00"),
                            CourtDetailSlot(displayTime = "09:00"),
                        ),
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_title").assertExists()
    composeRule.onNodeWithTag("court_detail_rating").assertExists()
    composeRule.onNodeWithTag("court_detail_location").assertExists()
    composeRule.onNodeWithTag("court_detail_disponibilidad_section").assertExists()
    composeRule.onNodeWithTag("court_detail_servicios_section").assertExists()
    composeRule.onNodeWithText("Parqueo").assertExists()
    composeRule.onNodeWithText("Iluminación").assertExists()
    composeRule.onNodeWithText("Hoy · slots de 1 hora").assertExists()
    composeRule.onNodeWithTag("court_detail_slot_08:00").assertExists()
    composeRule.onNodeWithTag("court_detail_slot_09:00").assertExists()
    composeRule.onNodeWithTag("court_detail_reserve_button").assertExists()
  }

  @Test
  fun disponibilidadSlotsArePresentedAsReadOnlyWithoutClickAffordance() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = listOf("Parqueo"),
            ratingAverage = 4.5,
            ratingCount = 8,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    isLoadingSlots = false,
                    availabilityHeadline = "Hoy · slots de 1 hora",
                    slots = listOf(CourtDetailSlot(displayTime = "18:00")),
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    // The informative availability time must read as read-only: it exposes no click
    // action, unlike the interactive selector in the reservation screen.
    composeRule.onNodeWithTag("court_detail_slot_18:00").assertExists().assertHasNoClickAction()
  }

  @Test
  fun detailScreenFallsBackToLocationPlaceholderWhenCoordinatesAreMissing() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            latitude = null,
            longitude = null,
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state = CourtDetailUiState(isLoadingSlots = false, slots = emptyList()),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_location_map").assertDoesNotExist()
    composeRule.onNodeWithText("Ubicación").assertExists()
  }

  @Test
  fun detailScreenRendersEmptyStateWhenNoSlotsAvailable() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state = CourtDetailUiState(isLoadingSlots = false, slots = emptyList()),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_no_slots_state").assertExists()
    composeRule.onNodeWithText("Sin horarios próximos").assertExists()
    composeRule
        .onNodeWithText(
            "No encontramos horarios disponibles en los próximos días. Tocá \"Reservar cancha\" para revisar más fechas."
        )
        .assertExists()
    composeRule.onNodeWithTag("court_detail_reserve_button").assertExists()
  }

  @Test
  fun detailScreenShowsFutureAvailabilityHeadlineWhenPreviewComesFromAnotherDay() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    isLoadingSlots = false,
                    availabilityHeadline = "Próximo día disponible · Jue, 2 de julio",
                    slots = listOf(CourtDetailSlot(displayTime = "18:00")),
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithText("Próximo día disponible · Jue, 2 de julio").assertExists()
    composeRule.onNodeWithTag("court_detail_slot_18:00").assertExists()
  }

  @Test
  fun detailScreenRendersLoadingStateForSlots() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state = CourtDetailUiState(isLoadingSlots = true),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_loading_slots", useUnmergedTree = true).assertExists()
    composeRule
        .onNodeWithTag("court_detail_loading_slots_indicator", useUnmergedTree = true)
        .assertExists()
    composeRule.onNodeWithText("Cargando disponibilidad…").assertExists()
    composeRule.onNodeWithTag("court_detail_no_slots_state").assertDoesNotExist()
  }

  @Test
  fun detailScreenRendersErrorStateWithRetryButton() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    isLoadingSlots = false,
                    slotsErrorMessage = "No pudimos cargar la disponibilidad.",
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_retry_slots_button").assertExists()
    composeRule.onNodeWithText("Sin disponibilidad").assertExists()
  }

  @Test
  fun detailScreenRendersReviewsSectionWithPublishedReviews() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = 4.5,
            ratingCount = 8,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    isLoadingSlots = false,
                    isLoadingReviews = false,
                    reviews =
                        listOf(
                            CourtReview(
                                id = "review-a",
                                rating = 5,
                                comment = "Cancha impecable, volvería.",
                                authorName = "Diego R.",
                                authorInitials = "DR",
                                dateLabel = "2 de julio de 2026",
                            ),
                        ),
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_resenas_section").assertExists()
    composeRule.onNodeWithTag("court_detail_review_review-a").assertExists()
    composeRule.onNodeWithText("Diego R.").assertExists()
    composeRule.onNodeWithText("Cancha impecable, volvería.").assertExists()
    composeRule.onNodeWithTag("court_detail_no_reviews_state").assertDoesNotExist()
  }

  @Test
  fun detailScreenRendersEmptyReviewsStateWhenCourtHasNoReviews() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    isLoadingSlots = false,
                    isLoadingReviews = false,
                    reviews = emptyList(),
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_no_reviews_state").assertExists()
    composeRule.onNodeWithText("Todavía no hay reseñas").assertExists()
  }

  @Test
  fun confirmedNotFavoriteShowsAccessibleOutlinedHeartOverHero() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    isLoadingSlots = false,
                    favoriteStatus = CourtFavoriteStatus.Confirmed,
                    isFavorite = false,
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule
        .onNodeWithTag("court_detail_favorite_button")
        .assert(hasAnyAncestor(hasTestTag("court_detail_hero")))
        .assertIsEnabled()
        .assertWidthIsAtLeast(48.dp)
        .assertHeightIsAtLeast(48.dp)
    composeRule
        .onNodeWithTag("court_detail_favorite_outlined_icon", useUnmergedTree = true)
        .assertExists()
    composeRule.onNodeWithContentDescription("Agregar cancha a favoritos").assertExists()
    composeRule
        .onAllNodesWithContentDescription(
            "Agregar cancha a favoritos",
            useUnmergedTree = true,
        )
        .assertCountEquals(1)
  }

  @Test
  fun confirmedFavoriteUsesFilledHeartAndUpdatingBlocksTaps() {
    var taps = 0
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    isLoadingSlots = false,
                    favoriteStatus = CourtFavoriteStatus.Updating,
                    isFavorite = true,
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
            onFavoriteToggle = { taps += 1 },
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_favorite_button").assertIsNotEnabled()
    composeRule
        .onNodeWithTag("court_detail_favorite_filled_icon", useUnmergedTree = true)
        .assertExists()
    composeRule.onNodeWithContentDescription("Quitar cancha de favoritos").assertExists()
    composeRule.runOnIdle { assertEquals(0, taps) }
  }

  @Test
  fun loadingDoesNotExposeAConfirmedHeartState() {
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state = CourtDetailUiState(favoriteStatus = CourtFavoriteStatus.Loading),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_favorite_button").assertIsNotEnabled()
    composeRule
        .onNodeWithTag("court_detail_favorite_loading", useUnmergedTree = true)
        .assertExists()
    composeRule
        .onNodeWithTag("court_detail_favorite_filled_icon", useUnmergedTree = true)
        .assertDoesNotExist()
    composeRule
        .onNodeWithTag("court_detail_favorite_outlined_icon", useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun initialFavoriteErrorShowsMessageAndRetriesFromTheOverlayControl() {
    var retries = 0
    composeRule.setContent {
      MejenguerosTheme {
        CourtDetailScreen(
            courtName = "Cancha 1",
            complexName = "Mejengas CR",
            provinceName = "San José",
            cantonName = "Escazú",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            state =
                CourtDetailUiState(
                    favoriteStatus = CourtFavoriteStatus.Error,
                    favoriteErrorMessage = "No pudimos cargar favoritos. Intentá nuevamente.",
                ),
            contentPadding = PaddingValues(),
            onReserve = {},
            onRetrySlots = {},
            onRetryReviews = {},
            onRetryFavorite = { retries += 1 },
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_favorite_error").assertExists()
    composeRule.onNodeWithContentDescription("Reintentar cargar favoritos").performClick()
    composeRule.runOnIdle { assertEquals(1, retries) }
  }
}
