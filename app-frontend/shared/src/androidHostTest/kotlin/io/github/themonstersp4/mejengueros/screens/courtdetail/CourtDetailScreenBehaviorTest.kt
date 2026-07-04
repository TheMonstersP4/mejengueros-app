package io.github.themonstersp4.mejengueros.screens.courtdetail

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailSlot
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
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
        )
      }
    }

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
        )
      }
    }

    composeRule.onNodeWithTag("court_detail_retry_slots_button").assertExists()
    composeRule.onNodeWithText("Sin disponibilidad").assertExists()
  }
}
